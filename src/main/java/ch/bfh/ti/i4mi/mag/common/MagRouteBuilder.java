package ch.bfh.ti.i4mi.mag.common;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.web.util.UriComponentsBuilder;

import static ch.bfh.ti.i4mi.mag.config.TlsConfiguration.BEAN_TLS_CONTEXT_WS;

public abstract class MagRouteBuilder extends RouteBuilder {
    private static final String UNEXPECTED_HTML_PART = "Incoming portion of HTML stream";

    protected final MagProps magProps;

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

    public Processor errorFromException() {
        return exchange -> {
            final var e = exchange.getIn().getBody(Exception.class);

            final String message;
            final String diagnostics;
            if (e instanceof final SOAPFaultException soapFault) {
                log.debug("SOAP Fault caught", soapFault);
                message = "Unexpected exception in SOAP transaction";

                String faultString = soapFault.getFault().getFaultString();
                if (faultString.contains(UNEXPECTED_HTML_PART)) {
                    faultString = faultString.substring(0, faultString.indexOf(UNEXPECTED_HTML_PART)).trim();
                }
                diagnostics = "Route %s, SOAP Fault: %s".formatted(exchange.getFromRouteId(), faultString);
            } else if (e instanceof final BaseServerResponseException hapiException) {
                // already a FHIR exception, just rethrow
                throw hapiException;
            } else {
                log.debug("Exception caught", e);
                message = "Unexpected exception in Camel route";
                diagnostics = "Route %s, exception %s".formatted(exchange.getFromRouteId(),
                                                                 e.getClass().getSimpleName());
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
}
