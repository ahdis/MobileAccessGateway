package ch.bfh.ti.i4mi.mag.common;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.cxf.interceptor.Fault;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.openehealth.ipf.commons.ihe.fhir.Constants;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ch.bfh.ti.i4mi.mag.config.TlsConfiguration.BEAN_TLS_CONTEXT_WS;

public abstract class MagRouteBuilder extends RouteBuilder {
    private static final String UNEXPECTED_HTML_PART = "Incoming portion of HTML stream";

    protected final MagProps magProps;
    protected final FhirContext fhirContext = FhirContext.forR4Cached();

    protected MagRouteBuilder(final MagProps magProps) {
        super();
        this.magProps = magProps;
    }

    protected String buildOutgoingEndpoint(final String scheme,
                                           final String uri,
                                           final boolean useTls) {
        final var builder = UriComponentsBuilder.fromUriString(uri)
                .scheme(scheme)
                .queryParam("secure", String.valueOf(useTls))
                .queryParam("audit", String.valueOf(this.magProps.getAtna().isAuditEnabled()));

        if (useTls) {
            builder.queryParam("sslContextParameters", "#" + BEAN_TLS_CONTEXT_WS);
        }

//        "&inInterceptors=#soapResponseLogger" +
//        "&inFaultInterceptors=#soapResponseLogger"+
//        "&outInterceptors=#soapRequestLogger" +
//        "&outFaultInterceptors=#soapRequestLogger";
        final var endpoint = builder.build().toUriString().replace(":/", "://");
        log.info("Built endpoint: {}", endpoint);
        return endpoint;
    }

    protected Processor errorFromException() {
        return exchange -> {
            final var e = exchange.getIn().getBody(Exception.class);
            log.debug("Generating response for exception", e);

            final String message;
            final String diagnostics;
            switch (e) {
                case SOAPFaultException soapFault -> {
                    log.debug("SOAP Fault caught", soapFault);
                    message = "Unexpected exception in SOAP transaction";

                    String faultString = soapFault.getFault().getFaultString();
                    if (faultString.contains(UNEXPECTED_HTML_PART)) {
                        faultString = faultString.substring(0, faultString.indexOf(UNEXPECTED_HTML_PART)).trim();
                    }
                    diagnostics = "Route %s, SOAP Fault: %s".formatted(exchange.getFromRouteId(), faultString);
                }
                case Fault fault -> {
                    log.debug("SOAP Fault caught", fault);
                    message = "Unexpected exception in SOAP transaction";

                    String faultString = fault.getMessage();
                    if (faultString.contains(UNEXPECTED_HTML_PART)) {
                        faultString = faultString.substring(0, faultString.indexOf(UNEXPECTED_HTML_PART)).trim();
                    }
                    diagnostics = "Route %s, SOAP Fault: %s".formatted(exchange.getFromRouteId(), faultString);
                }
                case BaseServerResponseException hapiException ->
                    // already a FHIR exception, just rethrow
                        throw hapiException;
                default -> {
                    log.debug("Exception caught", e);
                    message = "Unexpected exception in Camel route";
                    diagnostics = "Route %s, exception %s".formatted(exchange.getFromRouteId(),
                                                                     e.getClass().getSimpleName());
                }
            }

            final var oo = new OperationOutcome();
            final var issue = oo.addIssue();
            issue.setSeverity(OperationOutcome.IssueSeverity.FATAL);
            issue.setCode(OperationOutcome.IssueType.EXCEPTION);
            issue.setDetails(new CodeableConcept().setText(message));
            issue.setDiagnostics(diagnostics);

            throw new InternalErrorException(message, oo);
        };
    }

    protected static Processor loggingRequestProcessor(final LoggingLevel camelLoggingLevel, final Logger logger) {
        final var level = convertLoggingLevel(camelLoggingLevel);
        if (!logger.isEnabledForLevel(level)) {
            return _ -> {
            };
        } else {
            return exchange -> {
                final var body = exchange.getMessage().getBody();
                final var sb = new StringBuilder();
                final var headers = exchange.getMessage().getHeaders();

                sb.append(headers.get(Constants.HTTP_METHOD))
                        .append(" ")
                        .append(headers.get(Constants.HTTP_URL));
                final var query = headers.get(Constants.HTTP_QUERY);
                if (query != null) {
                    sb.append("?").append(query);
                }
                sb.append("\nHeaders:\n");

                final Map<String, List<String>> httpHeaders =
                        exchange.getIn().getHeader(Constants.HTTP_INCOMING_HEADERS, Collections::emptyMap, Map.class);
                httpHeaders.forEach((key, value) -> {
                    sb.append("  ").append(key).append("=");
                    switch (value.size()) {
                        case 0 -> sb.append("<no value>");
                        case 1 -> sb.append(value.getFirst());
                        default -> value.forEach(v -> sb.append("\n    - ").append(v));
                    }
                    sb.append("\n");
                });

                if (body == null) {
                    sb.append("<empty body>");
                } else if (body instanceof final Resource fhirResource) {
                    sb.append("Body:\n").append(FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(
                            fhirResource));
                } else {
                    sb.append("Body:\n").append(exchange.getMessage().getBody(String.class));
                }

                logger.atLevel(level).log(sb.toString());
            };
        }
    }

    protected static Processor loggingResponseProcessor(final LoggingLevel camelLoggingLevel, final Logger logger) {
        final var level = convertLoggingLevel(camelLoggingLevel);
        if (!logger.isEnabledForLevel(level)) {
            return _ -> {
            };
        } else {
            return exchange -> {
                final var body = exchange.getMessage().getBody();
                if (body == null) {
                    logger.atLevel(level).log("<empty body>");
                    return;
                }
                final String bodyString;
                if (body instanceof final Resource fhirResource) {
                    bodyString = FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(
                            fhirResource);
                } else if (body instanceof final Collection<?> collection) {
                    bodyString = "[" + collection.stream()
                            .map(item -> {
                                if (item instanceof final Resource itemResource) {
                                    return FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(
                                            itemResource);
                                } else {
                                    return item.toString();
                                }
                            })
                            .collect(Collectors.joining(",")) + "]";
                } else {
                    bodyString = exchange.getMessage().getBody(String.class);
                }
                logger.atLevel(level).log(bodyString);
            };
        }
    }

    private static Level convertLoggingLevel(LoggingLevel loggingLevel) {
        return switch (loggingLevel) {
            case TRACE -> Level.TRACE;
            case DEBUG -> Level.DEBUG;
            case INFO -> Level.INFO;
            case WARN -> Level.WARN;
            case ERROR -> Level.ERROR;
            default -> Level.INFO;
        };
    }
}
