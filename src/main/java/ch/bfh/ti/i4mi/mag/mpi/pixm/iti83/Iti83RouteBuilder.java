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

package ch.bfh.ti.i4mi.mag.mpi.pixm.iti83;

import ch.bfh.ti.i4mi.mag.common.MagRouteBuilder;
import ch.bfh.ti.i4mi.mag.common.PatientIdInterceptor;
import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import ch.bfh.ti.i4mi.mag.config.props.MagMpiProps;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import ch.bfh.ti.i4mi.mag.mhd.BaseResponseConverter;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import jakarta.xml.ws.soap.SOAPFaultException;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

/**
 *
 */
@Slf4j
@Component
@ConditionalOnProperty("mag.mpi.iti-45")
class Iti83RouteBuilder extends MagRouteBuilder {

    private final MagMpiProps mpiProps;
    private final Iti83ResponseConverter responseConverter;

    public Iti83RouteBuilder(final MagProps magProps,
                             final Iti83ResponseConverter responseConverter) {
        super(magProps);
        this.mpiProps = magProps.getMpi();
        this.responseConverter = responseConverter;
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti83RouteBuilder configure");

        final String xds45Endpoint = this.buildOutgoingEndpoint("pixv3-iti45",
                                                                this.mpiProps.getIti45(),
                                                                this.mpiProps.isHttps());

        from("pixm-iti83:mobile-patient-identifier-cross-reference-query?audit=false")
                .routeId("in-pixm-iti83")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                //.process(RequestHeadersForwarder.checkAuthorization(this.mpiProps.isChPixmConstraints()))
                .process(RequestHeadersForwarder.forward())
                .process(Utils.keepBody())
                .bean(Iti83RequestConverter.class)
                .doTry()
                    .to(xds45Endpoint)
                    .process(Utils.keptBodyToHeader())
                    .process(TraceparentHandler.updateHeaderForFhir())
                    .process(translateToFhir(responseConverter, byte[].class))
                    .bean(PatientIdInterceptor.class, "interceptIti83Parameters")
                .doCatch(Exception.class)
                    .setBody(simple("${exception}"))
                    .process(this.errorFromException())
                .end();

    }
}
