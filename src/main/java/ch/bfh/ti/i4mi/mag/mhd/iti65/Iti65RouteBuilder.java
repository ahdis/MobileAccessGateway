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
import ch.bfh.ti.i4mi.mag.common.MagRouteBuilder;
import ch.bfh.ti.i4mi.mag.common.PatientIdMappingService;
import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import ch.bfh.ti.i4mi.mag.config.props.MagXdsProps;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import org.apache.camel.Body;
import org.apache.camel.Processor;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.ProvideAndRegisterDocumentSetRequestType;
import org.openehealth.ipf.commons.ihe.xds.core.requests.ProvideAndRegisterDocumentSet;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.method.annotation.RequestHeaderMethodArgumentResolver;
import org.w3c.dom.Element;

import java.util.Optional;

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

        from("mhd-iti65:provide-document-bundle?fhirContext=#fhirContext&audit=false")
                .routeId("in-mhd-iti65")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                //.process(itiRequestValidator())
                //.process(RequestHeadersForwarder.checkAuthorization(this.xdsProps.isChMhdConstraints()))
                .process(RequestHeadersForwarder.forward())
                // translate, forward, translate back
                .process(Utils.keepBody())
                .process(Utils.storeBodyToHeader("BundleRequest"))
                .bean(Iti65RequestConverter.class)
                .process(Utils.storeBodyToHeader("ProvideAndRegisterDocumentSet"))
                .choice()
                    .when(header(AUTHORIZATION_HEADER).isNull())
                        .log("No Authorization header present, forwarding without")
                        .process(injectTcuXuaProcessor())
                .end()
                //.convertBodyTo(ProvideAndRegisterDocumentSetRequestType.class)
                //.process(iti41RequestValidator())
                .to(xds41Endpoint)
                .convertBodyTo(Response.class)
                .process(TraceparentHandler.updateHeaderForFhir())
                .process(translateToFhir(this.iti65ResponseConverter, Response.class));
    }

    private Processor injectTcuXuaProcessor() {
        if (this.tcuXuaService != null) {
            return exchange -> {
                final var body = exchange.getIn().getBody(ProvideAndRegisterDocumentSet.class);
                final var xadPid = body.getSubmissionSet().getPatientId().getId();
                final var eprSpid = "761337614808965105"; //this.patientIdMappingService.getEprSpid(xadPid);
                log.debug("EPR SPID: {}", eprSpid);
                final var tcuXua = this.tcuXuaService.getXuaToken(eprSpid);
                RequestHeadersForwarder.setWsseHeader(exchange, tcuXua);
            };
        }
        return exchange -> {};
    }

     /*
    private class Responder extends ExpressionAdapter {

        @Override
        public Object evaluate(Exchange exchange) {
            Bundle requestBundle = exchange.getIn().getBody(Bundle.class);

            Bundle responseBundle = new Bundle()
                    .setType(Bundle.BundleType.TRANSACTIONRESPONSE)
                    .setTotal(requestBundle.getTotal());

            for (Bundle.BundleEntryComponent requestEntry : requestBundle.getEntry()) {
                Bundle.BundleEntryResponseComponent response = new Bundle.BundleEntryResponseComponent()
                        .setStatus("201 Created")
                        .setLastModified(new Date())
                        .setLocation(requestEntry.getResource().getClass().getSimpleName() + "/" + 4711);
                responseBundle.addEntry()
                        .setResponse(response)
                        .setResource(responseResource(requestEntry.getResource()));
            }
            return responseBundle;
        }

    }

    private Resource responseResource(Resource request) {
        if (request instanceof DocumentManifest) {
            return new DocumentManifest().setId(UUID.randomUUID().toString());
        } else if (request instanceof DocumentReference) {
            return new DocumentReference().setId(UUID.randomUUID().toString());
        } else if (request instanceof ListResource) {
            return new ListResource().setId(UUID.randomUUID().toString());
        } else if (request instanceof Binary) {
            return new Binary().setId(UUID.randomUUID().toString());
        } else {
            throw new IllegalArgumentException(request + " is not allowed here");
        }
    }
    */
}
