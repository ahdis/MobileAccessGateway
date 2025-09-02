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

package ch.bfh.ti.i4mi.mag.mpi.pdqm.iti78;

import ch.bfh.ti.i4mi.mag.common.MagRouteBuilder;
import ch.bfh.ti.i4mi.mag.common.PatientIdInterceptor;
import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import ch.bfh.ti.i4mi.mag.config.props.MagMpiProps;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import ch.bfh.ti.i4mi.mag.mhd.BaseResponseConverter;
import lombok.extern.slf4j.Slf4j;
import org.openehealth.ipf.commons.ihe.fhir.Constants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

/**
 * IHE PDQM: ITI-78 Patient Demographics Query
 */
@Slf4j
@Component
@ConditionalOnProperty("mag.mpi.iti-47")
public class Iti78RouteBuilder extends MagRouteBuilder {

    private final MagMpiProps mpiProps;
    private final Iti78ResponseConverter responseConverter;

    public Iti78RouteBuilder(final MagProps magProps,
                             final Iti78ResponseConverter responseConverter) {
        super(magProps);
        this.mpiProps = magProps.getMpi();
        this.responseConverter = responseConverter;
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti78RouteBuilder configure");

        final String xds47Endpoint = this.buildOutgoingEndpoint("pdqv3-iti47",
                                                                this.mpiProps.getIti47(),
                                                                this.mpiProps.isHttps());

        from("pdqm-iti78:mobile-patient-demographics-query?audit=false")
                .routeId("in-pdqm-iti78")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                //.process(RequestHeadersForwarder.checkAuthorization(this.mpiProps.isChPdqmConstraints()))
                .process(RequestHeadersForwarder.forward())
                .choice()
                .when(header(Constants.FHIR_REQUEST_PARAMETERS).isNotNull())
                .bean(Iti78RequestConverter.class, "iti78ToIti47Converter")
                .endChoice()
                .when(header("FhirHttpUri").isNotNull())
                .bean(Iti78RequestConverter.class, "idConverter")
                .endChoice()
                .end()
                .doTry()
                .to(xds47Endpoint)
                .process(TraceparentHandler.updateHeaderForFhir())
                .process(translateToFhir(responseConverter, byte[].class))
                .bean(PatientIdInterceptor.class, "interceptBundleOfPatients")
                .doCatch(jakarta.xml.ws.soap.SOAPFaultException.class)
                .setBody(simple("${exception}"))
                .bean(BaseResponseConverter.class, "errorFromException")
                .end();

    }
}
