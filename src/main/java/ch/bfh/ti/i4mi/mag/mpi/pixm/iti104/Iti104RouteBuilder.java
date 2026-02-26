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

package ch.bfh.ti.i4mi.mag.mpi.pixm.iti104;

import ch.bfh.ti.i4mi.mag.common.MagRouteBuilder;
import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import ch.bfh.ti.i4mi.mag.config.props.MagMpiProps;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import ch.bfh.ti.i4mi.mag.mpi.common.Iti47ResponseToFhirConverter;
import ch.bfh.ti.i4mi.mag.mpi.pdqm.iti78.Iti78RequestConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.LoggingLevel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

/**
 *
 */
@Slf4j
@Component
@ConditionalOnProperty({"mag.mpi.iti-44", "mag.mpi.iti-47"})
class Iti104RouteBuilder extends MagRouteBuilder {

    private final MagMpiProps mpiProps;
    private final Iti104ResponseConverter response104Converter;

    public Iti104RouteBuilder(final MagProps magProps,
                              final Iti104ResponseConverter response104Converter) {
        super(magProps);
        this.mpiProps = magProps.getMpi();
        this.response104Converter = response104Converter;
    }

    @Override
    public void configure() throws Exception {
        log.debug("Configuring ITI-104 route");

        final String xds44Endpoint = this.buildOutgoingEndpoint("pixv3-iti44",
                                                                this.mpiProps.getIti44(),
                                                                this.mpiProps.isHttps());
        final String xds47Endpoint = this.buildOutgoingEndpoint("pdqv3-iti47",
                                                                this.mpiProps.getIti47(),
                                                                this.mpiProps.isHttps());

        // @formatter:off
        from("pixm-iti104:patient-identity-feed-fhir?audit=false")
                .routeId("in-pixm-iti104")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                .log(LoggingLevel.INFO, log, "Received ITI-104 request")
                .process(loggingRequestProcessor(LoggingLevel.TRACE, log))
                //.process(RequestHeadersForwarder.checkAuthorization(this.mpiProps.isChPdqmConstraints()))
                .process(RequestHeadersForwarder.forward())
                .process(Utils.keepBody())
                .bean(Iti104RequestConverter.class)
                .doTry()
                    .log(LoggingLevel.DEBUG, log, "Sending an ITI-44 request to " + xds44Endpoint)
                    .log(LoggingLevel.TRACE, log, "${body}")
                    .to(xds44Endpoint)
                    .log(LoggingLevel.DEBUG, log, "Got a response")
                    .log(LoggingLevel.TRACE, log, "${body}")
                    .process(Utils.keptBodyToHeader())
                    .process(Utils.storePreferHeader())
                    .process(translateToFhir(this.response104Converter, byte[].class))
                    .process(TraceparentHandler.updateHeaderForFhir())
                    .choice()
                        .when(header("Prefer").isEqualToIgnoreCase("return=Representation"))
                            .process(Utils.keepBody())
                            .bean(Iti78RequestConverter.class, "fromMethodOutcome")
                            .process(TraceparentHandler.updateHeaderForSoap())
                            .log(LoggingLevel.DEBUG, log, "Sending an ITI-47 request to " + xds44Endpoint)
                            .log(LoggingLevel.TRACE, log, "${body}")
                            .to(xds47Endpoint)
                            .log(LoggingLevel.DEBUG, log, "Got a response")
                            .log(LoggingLevel.TRACE, log, "${body}")
                            .bean(Iti47ResponseToFhirConverter.class, "convertForIti104")
                            .process(TraceparentHandler.updateHeaderForFhir())
                            .process(Iti104ResponseConverter.addPatientToOutcome())
                        .endChoice()
                    .end()
                    .log(LoggingLevel.DEBUG, log, "Finished generating the ITI-104 response")
                    .process(loggingResponseProcessor(LoggingLevel.TRACE, log))
                .endDoTry()
                .doCatch(Exception.class)
                    .setBody(simple("${exception}"))
                    .process(this.errorFromException())
                .end();
        // @formatter:on
    }
}
