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

import ch.bfh.ti.i4mi.mag.common.MagRouteBuilder;
import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import ch.bfh.ti.i4mi.mag.config.props.MagXdsProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * IHE MHD: Retrieve Document [ITI-68] for Document Responder see also https://oehf.github.io/ipf-docs/docs/ihe/iti68/
 * https://oehf.github.io/ipf-docs/docs/boot-fhir/ https://camel.apache.org/components/latest/servlet-component.html
 */
@Component
@ConditionalOnProperty("mag.xds.iti-43")
class Iti68RouteBuilder extends MagRouteBuilder {
    private static final Logger log = LoggerFactory.getLogger(Iti68RouteBuilder.class);

    private final MagXdsProps xdsProps;
    private final boolean isChMhdConstraints;

    public Iti68RouteBuilder(final MagProps magProps) {
        super(magProps);
        this.xdsProps = magProps.getXds();
        this.isChMhdConstraints = xdsProps.isChMhdConstraints();
        log.debug("Iti68RouteBuilder initialized");
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti68RouteBuilder configure");
        final String xds43Endpoint = this.buildOutgoingEndpoint("xds-iti43",
                                                                this.xdsProps.getIti43(),
                                                                this.xdsProps.isHttps());

        from("mhd-iti68:camel/xdsretrieve?audit=false")
                .routeId("in-mhd-iti68")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                .doTry()
                    //.process(RequestHeadersForwarder.checkAuthorization(this.isChMhdConstraints))
                    .process(RequestHeadersForwarder.forward())

                    // translate, forward, translate back
                    .bean(Iti68RequestConverter.class)
                    .to(xds43Endpoint)
                    .process(TraceparentHandler.updateHeaderForFhir())
                    .bean(Iti68ResponseConverter.class, "retrievedDocumentSetToHttResponse")
                .doCatch(Exception.class)
                    .setBody(simple("${exception}"))
                    .process(this.errorFromException())
                .end();
        // if removing retrievedDocumentSetToHttResponse its given an AmbiguousMethodCallException with two same methods??
        // public java.lang.Object ch.bfh.ti.i4mi.mag.mhd.iti68.Iti68ResponseConverter.retrievedDocumentSetToHttResponse(org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocumentSet,java.util.Map) throws java.io.IOException,
        // public java.lang.Object ch.bfh.ti.i4mi.mag.mhd.iti68.Iti68ResponseConverter.retrievedDocumentSetToHttResponse(org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocumentSet,java.util.Map) throws java.io.IOException]
    }
}
