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

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import ch.bfh.ti.i4mi.mag.config.props.MagMpiProps;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.ExpressionAdapter;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ch.bfh.ti.i4mi.mag.mhd.BaseResponseConverter;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import ch.bfh.ti.i4mi.mag.mpi.pdqm.iti78.Iti78RequestConverter;
import ch.bfh.ti.i4mi.mag.mpi.pdqm.iti78.Iti78ResponseConverter;
import lombok.extern.slf4j.Slf4j;
 
/**
 * 
 */
@Slf4j
@Component
@ConditionalOnProperty({"mag.mpi.iti-44", "mag.mpi.iti-47"})
class Iti104RouteBuilder extends RouteBuilder {

    private final MagMpiProps mpiProps;
    private final Iti104ResponseConverter response104Converter;
    private final Iti78ResponseConverter response78Converter;

    public Iti104RouteBuilder(final MagMpiProps mpiProps,
                              final Iti104ResponseConverter response104Converter,
                              final Iti78ResponseConverter response78Converter) {
        super();
        this.mpiProps = mpiProps;
        this.response104Converter = response104Converter;
        this.response78Converter = response78Converter;
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti104RouteBuilder configure");
        
        final String xds44Endpoint = String.format("pixv3-iti44://%s" +
                "?secure=%s", this.mpiProps.getIti44(), this.mpiProps.isHttps() ? "true" : "false")
                +
                "&audit=true" +
                "&auditContext=#auditContext" +
              //  "&sslContextParameters=#pixContext" +
                "&inInterceptors=#soapResponseLogger" + 
                "&inFaultInterceptors=#soapResponseLogger"+
                "&outInterceptors=#soapRequestLogger" + 
                "&outFaultInterceptors=#soapRequestLogger";
        
   	 final String xds47Endpoint = String.format("pdqv3-iti47://%s" +
             "?secure=%s", this.mpiProps.getIti47(), this.mpiProps.isHttps() ? "true" : "false")
             +
             //"&sslContextParameters=#pixContext" +
             "&audit=true" +
             "&auditContext=#auditContext" +
             "&inInterceptors=#soapResponseLogger" + 
             "&inFaultInterceptors=#soapResponseLogger"+
             "&outInterceptors=#soapRequestLogger" + 
             "&outFaultInterceptors=#soapRequestLogger";
        
        
        from("pixm-iti104:stub?audit=true&auditContext=#auditContext").routeId("iti104-feed")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                //.process(RequestHeadersForwarder.checkAuthorization(this.mpiProps.isChPdqmConstraints()))
                .process(RequestHeadersForwarder.forward())
                .process(Utils.keepBody())                
                .bean(Iti104RequestConverter.class)
                .doTry()
                  .to(xds44Endpoint)
                  .process(Utils.keptBodyToHeader())
                  .process(Utils.storePreferHeader())
                  .process(translateToFhir(this.response104Converter , byte[].class))
                  .process(TraceparentHandler.updateHeaderForFhir())
                  .choice()
	                    .when(header("Prefer").isEqualToIgnoreCase("return=Representation"))	                    
	                    .process(Utils.keepBody())
	                    .bean(Iti78RequestConverter.class, "fromMethodOutcome")
                        .process(TraceparentHandler.updateHeaderForSoap())
	                    .to(xds47Endpoint)
	  			        .process(translateToFhir(this.response78Converter , byte[].class))
                        .process(TraceparentHandler.updateHeaderForFhir())
	  			        .process(Iti104ResponseConverter.addPatientToOutcome())
	  			        .endChoice()
                  .end()                     
                 .endDoTry()
            	.doCatch(jakarta.xml.ws.soap.SOAPFaultException.class)
				  .setBody(simple("${exception}"))
				  .bean(BaseResponseConverter.class, "errorFromException")
				.end();
                
    }

    private class Responder extends ExpressionAdapter {

        @Override
        public Object evaluate(Exchange exchange) {
            Bundle requestBundle = exchange.getIn().getBody(Bundle.class);
            
            Bundle responseBundle = new Bundle()
                    .setType(Bundle.BundleType.TRANSACTIONRESPONSE)
                    .setTotal(requestBundle.getTotal());

            return responseBundle;
        }

    }

}
