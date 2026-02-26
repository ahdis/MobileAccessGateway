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

package ch.bfh.ti.i4mi.mag.mhd.iti66;

import ch.bfh.ti.i4mi.mag.common.MagRouteBuilder;
import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import ch.bfh.ti.i4mi.mag.config.props.MagXdsProps;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import org.apache.camel.LoggingLevel;
import org.openehealth.ipf.commons.ihe.fhir.Constants;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.slf4j.Logger;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

/**
 * IHE MHD: Find Document Manifests [ITI-66] for Document Responder https://oehf.github.io/ipf-docs/docs/ihe/iti66/
 */
@Component
@Conditional(Iti66Condition.class)
class Iti66RouteBuilder extends MagRouteBuilder {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Iti66RouteBuilder.class);
    private final MagXdsProps xdsProps;
    private final Iti66ResponseConverter iti66ResponseConverter;

    public Iti66RouteBuilder(final MagProps magProps,
                             final Iti66ResponseConverter iti66ResponseConverter) {
        super(magProps);
        this.xdsProps = magProps.getXds();
        this.iti66ResponseConverter = iti66ResponseConverter;
    }

    @Override
    public void configure() throws Exception {
        log.debug("Configuring ITI-66 route");
        final String xds18Endpoint = this.buildOutgoingEndpoint("xds-iti18",
                                                                this.xdsProps.getIti18(),
                                                                this.xdsProps.isHttps());

        // @formatter:off
        from("mhd-iti66:find-document-lists?audit=false")
                .routeId("in-mhd-iti66")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                .log(LoggingLevel.INFO, log, "Received ITI-66 request")
                .process(loggingRequestProcessor(LoggingLevel.TRACE, log))
                //.process(RequestHeadersForwarder.checkAuthorization(this.xdsProps.isChMhdConstraints()))
                .process(RequestHeadersForwarder.forward())
                .choice()
                    .when(header(Constants.FHIR_REQUEST_PARAMETERS).isNotNull())
                        .bean(Utils.class, "searchParameterToBody")
                        .bean(Iti66RequestConverter.class)
                    .endChoice()
                    .when(header("FhirHttpUri").isNotNull())
                        .bean(IdRequestConverter.class)
                    .endChoice()
                .end()
                .doTry()
                    .log(LoggingLevel.DEBUG, log, "Sending an ITI-18 request to " + xds18Endpoint)
                    .log(LoggingLevel.TRACE, log, "${body}")
                    .to(xds18Endpoint)
                    .log(LoggingLevel.DEBUG, log, "Got a response")
                    .log(LoggingLevel.TRACE, log, "${body}")
                    .bean(Iti66ResponseBugfix.class)
                    .process(TraceparentHandler.updateHeaderForFhir())
                    .process(translateToFhir(this.iti66ResponseConverter, QueryResponse.class))
                    .log(LoggingLevel.DEBUG, log, "Finished generating the ITI-66 response")
                    .process(loggingResponseProcessor(LoggingLevel.TRACE, log))
                .doCatch(Exception.class)
                    .setBody(simple("${exception}"))
                    .process(this.errorFromException())
                .end();
        // @formatter:on
    }
}
