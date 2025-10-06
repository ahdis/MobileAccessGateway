/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.bfh.ti.i4mi.mag.mhd.iti65;

import ch.bfh.ti.i4mi.mag.auth.TcuXuaService;
import ch.bfh.ti.i4mi.mag.common.*;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import ch.bfh.ti.i4mi.mag.config.props.MagXdsProps;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.requests.ProvideAndRegisterDocumentSet;
import org.openehealth.ipf.commons.ihe.xds.core.responses.ErrorCode;
import org.openehealth.ipf.commons.ihe.xds.core.responses.ErrorInfo;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Predicate;

import static ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder.AUTHORIZATION_HEADER;
import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

/**
 * IHE MHD: [ITI-65] Provide Document Bundle Request Message for Document Recipient
 * https://oehf.github.io/ipf-docs/docs/ihe/iti65/
 */
@Component
@ConditionalOnProperty("mag.xds.iti-41")
class Iti65RouteBuilder extends MagRouteBuilder {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Iti65RouteBuilder.class);
    private static final String REQUESTED_CONF_CODE = "requestedConfidentialityCode";
    private static final String CONF_CODE_IS_NORMAL = generateSimpleConfCodeCondition(ConfidentialityCode.NORMAL);
    private static final String CONF_CODE_IS_RESTRICTED = generateSimpleConfCodeCondition(ConfidentialityCode.RESTRICTED);
    private static final String CONF_CODE_IS_SECRET = generateSimpleConfCodeCondition(ConfidentialityCode.SECRET);

    private final MagXdsProps xdsProps;
    private final Iti65ResponseConverter iti65ResponseConverter;
    private final TcuXuaService tcuXuaService;
    private final PatientIdMappingService patientIdMappingService;

    public Iti65RouteBuilder(final MagProps magProps,
                             final Iti65ResponseConverter iti65ResponseConverter,
                             final Optional<TcuXuaService> tcuXuaService,
                             final PatientIdMappingService patientIdMappingService) {
        super(magProps);
        this.xdsProps = magProps.getXds();
        this.iti65ResponseConverter = iti65ResponseConverter;
        this.tcuXuaService = tcuXuaService.orElse(null);
        this.patientIdMappingService = patientIdMappingService;
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti65RouteBuilder configure");

        final String xds41Endpoint = this.buildOutgoingEndpoint("xds-iti41",
                                                                this.xdsProps.getIti41(),
                                                                this.xdsProps.isHttps());

        // @formatter:off
        from("mhd-iti65:provide-document-bundle?fhirContext=#fhirContext&audit=false")
                .routeId("in-mhd-iti65")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                .doTry()
                    //.process(itiRequestValidator())
                    //.process(RequestHeadersForwarder.checkAuthorization(this.xdsProps.isChMhdConstraints()))
                    // translate, forward, translate back
                    .process(Utils.storeBodyToHeader("BundleRequest"))
                    .bean(Iti65RequestConverter.class)
                    .process(maybeInjectTcuXuaProcessor())
                    .process(RequestHeadersForwarder.forward())

                    .convertBodyTo(ProvideAndRegisterDocumentSet.class)
                    .process(Utils.keepBody())
                    .process(Utils.storeBodyToHeader("ProvideAndRegisterDocumentSet"))
                    //.process(iti41RequestValidator())

                    // Determine the confidentiality code requested
                    .setProperty(REQUESTED_CONF_CODE).exchange(this::findConfidentialityCode)

                    // Let's loop on all possible confidentiality codes, and change the current code if a publication is
                    // refused with an error message compatible with a confidentiality code mismatch
                    .choice().when().simple(CONF_CODE_IS_NORMAL)
                        .log("In ITI-65 route, confidentiality code is NORMAL")
                        .to(xds41Endpoint)
                        .convertBodyTo(Response.class)
                        .process(this.changeConfidentialityCodeIfNeeded())
                    .endDoTry()

                    .choice().when().simple(CONF_CODE_IS_RESTRICTED)
                        .log("In ITI-65 route, confidentiality code is RESTRICTED")
                        .process(Utils.restoreKeptBody())
                        .process(forceConfidentialityCode())
                        .to(xds41Endpoint)
                        .process(this.changeConfidentialityCodeIfNeeded())
                    .endDoTry()

                    .choice().when().simple(CONF_CODE_IS_SECRET)
                        .log("In ITI-65 route, confidentiality code is SECRET")
                        .process(Utils.restoreKeptBody())
                        .process(forceConfidentialityCode())
                        .to(xds41Endpoint)
                    .endDoTry()

                    .process(TraceparentHandler.updateHeaderForFhir())
                    .process(translateToFhir(this.iti65ResponseConverter, Response.class))
                .doCatch(Exception.class)
                    .setBody(simple("${exception}"))
                    .process(this.errorFromException())
                .end();
        // @formatter:on
    }

    private Processor maybeInjectTcuXuaProcessor() {
        if (this.tcuXuaService != null) {
            return exchange -> {
                final var authorizationHeader = FhirExchanges.readRequestHttpHeader(AUTHORIZATION_HEADER,
                                                                                    exchange,
                                                                                    false);
                if (authorizationHeader != null) {
                    return;
                }
                log.debug("Authorization header absent, injecting TCU XUA");

                final var body = exchange.getIn().getBody(ProvideAndRegisterDocumentSet.class);
                final var xadPid = body.getSubmissionSet().getPatientId().getId();
                final var eprSpid = this.patientIdMappingService.getEprSpid(xadPid);
                log.debug("EPR SPID: {}", eprSpid);
                final var tcuXua = this.tcuXuaService.getXuaToken(eprSpid);
                RequestHeadersForwarder.setWsseHeader(exchange, tcuXua);
            };
        }
        return _ -> {
        };
    }


    public ConfidentialityCode findConfidentialityCode(final Exchange exchange) {
        final ProvideAndRegisterDocumentSet request = exchange.getMessage().getBody(ProvideAndRegisterDocumentSet.class);
        if (request.getDocuments().isEmpty()) {
            throw new IllegalStateException("No documents found in ProvideAndRegisterDocumentSet.");
        }
        if (request.getDocuments().getFirst().getDocumentEntry().getConfidentialityCodes().isEmpty()) {
            throw new IllegalStateException("No confidentiality codes found in DocumentEntry.");
        }
        final Code code = request.getDocuments().getFirst().getDocumentEntry().getConfidentialityCodes().getFirst();
        return ConfidentialityCode.from(code.getCode(), code.getSchemeName());
    }

    public Processor changeConfidentialityCodeIfNeeded() {
        return exchange -> {
            final Response response = exchange.getMessage().getBody(Response.class);
            final ConfidentialityCode currentCode = exchange.getProperty(REQUESTED_CONF_CODE,
                                                                         ConfidentialityCode.class);

            if (response.getStatus() != Status.FAILURE) {
                return;
            }

            // An ITH ErrorInfo for confidentiality code mismatch looks like this:
            // <ns6:RegistryError codeContext="Unexpected registry error." errorCode="XDSRegistryError" severity="urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Error" location="Sense XDS Registry"/>
            final Predicate<ErrorInfo> isIthCompatibleError =
                    (errorInfo) -> "Unexpected registry error.".equals(errorInfo.getCodeContext())
                            && "Sense XDS Registry".equals(errorInfo.getLocation())
                            && errorInfo.getErrorCode() == ErrorCode.REGISTRY_ERROR;

            // An Emedo ErrorInfo for confidentiality code mismatch looks like this (TODO verify):
            // <rs:RegistryError codeContext="Consent filter applied" errorCode="XDSRepositoryError" location="" severity="urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Error"/>
            final Predicate<ErrorInfo> isEmedoCompatibleError =
                    (errorInfo) -> "Consent filter applied".equals(errorInfo.getCodeContext())
                            && errorInfo.getErrorCode() == ErrorCode.REPOSITORY_ERROR;

            if (response.getErrors().stream().anyMatch(isIthCompatibleError) || response.getErrors().stream().anyMatch(
                    isEmedoCompatibleError)) {
                final var newCode = switch (currentCode) {
                    case NORMAL -> ConfidentialityCode.RESTRICTED;
                    case RESTRICTED -> ConfidentialityCode.SECRET;
                    case SECRET -> throw new IllegalStateException("Cannot increase confidentiality code above SECRET");
                };
                exchange.setProperty(REQUESTED_CONF_CODE, newCode);
            }
        };
    }

    public Processor forceConfidentialityCode() {
        return exchange -> {
            final ConfidentialityCode currentCode = exchange.getProperty(REQUESTED_CONF_CODE,
                                                                         ConfidentialityCode.class);
            final ProvideAndRegisterDocumentSet body = exchange.getIn().getBody(ProvideAndRegisterDocumentSet.class);
            final Code code = body.getDocuments().getFirst().getDocumentEntry().getConfidentialityCodes().getFirst();
            code.setCode(currentCode.getCode());
            code.setSchemeName(currentCode.getSystem());
        };
    }

    private static String generateSimpleConfCodeCondition(final ConfidentialityCode level) {
        return String.format(
                "${exchangeProperty.%s} == ${type:%s.%s.%s}",
                REQUESTED_CONF_CODE,
                ConfidentialityCode.class.getPackageName(),
                ConfidentialityCode.class.getSimpleName(),
                level.name()
        );
    }
}
