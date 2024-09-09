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

package ch.bfh.ti.i4mi.mag.mhd.iti68;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.MagRouteBuilder;
import ch.bfh.ti.i4mi.mag.xua.AuthTokenConverter;
import lombok.extern.slf4j.Slf4j;
import org.openehealth.ipf.commons.ihe.xds.core.requests.RetrieveDocumentSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * IHE MHD: Retrieve Document [ITI-68] for Document Responder see also
 * https://oehf.github.io/ipf-docs/docs/ihe/iti68/
 * https://oehf.github.io/ipf-docs/docs/boot-fhir/
 * https://camel.apache.org/components/latest/servlet-component.html
 */
@Slf4j
@Component
@ConditionalOnProperty("mag.xds.iti-43.url")
class Iti68RouteBuilder extends MagRouteBuilder {

    private static final String PROP_FOREIGN_COMMUNITY = Iti68RouteBuilder.class.getName() + ".foreign-community";

    private final Iti68RequestConverter iti68RequestConverter;

    @Autowired
    public Iti68RouteBuilder(final Config config, Iti68RequestConverter iti68RequestConverter) {
        super(config);
        log.debug("Iti68RouteBuilder initialized");
        this.iti68RequestConverter = iti68RequestConverter;
    }


    @Override
    public void configure() throws Exception {
        log.debug("Iti68RouteBuilder configure");
        final String xds43Endpoint = createSoapEndpointUri("xds-iti43", this.config.getIti43HostUrl());
        final String xds43EndpointInitGw = createSoapEndpointUri("xds-iti43", this.config.getIti43HostUrlInitGw());

        from("mhd-iti68:camel/xdsretrieve?audit=true&auditContext=#myAuditContext").routeId("ddh-retrievedoc-adapter")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                .process(AuthTokenConverter.addWsHeader())
               
                // translate, forward, translate back
                .process(exchange -> {
                    RetrieveDocumentSet retrieveDocumentSet = iti68RequestConverter.queryParameterToRetrieveDocumentSet(exchange.getIn().getHeaders());
                    if (retrieveDocumentSet.getDocuments().get(0).getHomeCommunityId() != null) {
                        log.debug("Will forward the ITI-43 request to the XCA Initiating Gateway");
                        exchange.setProperty(PROP_FOREIGN_COMMUNITY, Boolean.TRUE);
                    } else {
                        // TODO: Support multiple local repositories?  Otherwise, the MAG may be usable only in Swiss Post EPR environments.
                        log.debug("Will forward the ITI-43 request to the local Document Repository");
                    }
                    exchange.getMessage().setBody(retrieveDocumentSet);
                })
                .choice()
                    .when(exchangeProperty(PROP_FOREIGN_COMMUNITY).isNotNull())
                        .to(xds43EndpointInitGw)
                    .otherwise()
                        .to(xds43Endpoint)
                .end()
                .bean(Iti68ResponseConverter.class, "retrievedDocumentSetToHttResponse");
                // if removing retrievedDocumentSetToHttResponse its given an AmbiguousMethodCallException with two same methods??
                // public java.lang.Object ch.bfh.ti.i4mi.mag.mhd.iti68.Iti68ResponseConverter.retrievedDocumentSetToHttResponse(org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocumentSet,java.util.Map) throws java.io.IOException,
                // public java.lang.Object ch.bfh.ti.i4mi.mag.mhd.iti68.Iti68ResponseConverter.retrievedDocumentSetToHttResponse(org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocumentSet,java.util.Map) throws java.io.IOException] 
    }

}
