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

package ch.bfh.ti.i4mi.mag.sts;

import ch.bfh.ti.i4mi.mag.common.MagRouteBuilder;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class AssertionFromIdpTokenRouteBuilder extends MagRouteBuilder {

    private final StsUtils utils;

    public AssertionFromIdpTokenRouteBuilder(final MagProps magProps, final StsUtils utils) {
        super(magProps);
        this.utils = utils;
    }

    @Override
    public void configure() throws Exception {
        log.debug("Configuring IUA-STS route");

        // @formatter:off
        from("servlet://assertion?httpMethodRestrict=POST&matchOnUriPrefix=true")
                .routeId("iua-sts")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                .log(LoggingLevel.INFO, log, "Received IUA-STS request")
                .process(loggingRequestProcessor(LoggingLevel.TRACE, log))
                .doTry()
                    // end spring security session in order to prevent use of already expired
                    // identity provider assertions cached in spring security session
                    // this is unrelated to the IDP provider cookie set by the IDP itself
                    .process(Utils.endHttpSession())
                    .setProperty("oauthrequest").method(this.utils, "emptyAuthRequest")
                    .bean(this.utils, "buildAssertionRequestFromIdp")
                    .bean(this.utils, "keepIdpAssertion")
                    .bean(Iti40RequestGenerator.class, "buildAssertion")
                    .removeHeaders("*", "scope")
                    .to("direct:sts")
                    .bean(this.utils, "generateOAuth2TokenResponse")
                    .log(LoggingLevel.DEBUG, log, "Finished generating the IUA-STS response")
                    .process(loggingResponseProcessor(LoggingLevel.TRACE, log))
                .doCatch(AuthException.class)
                    .setBody(simple("${exception}"))
                    .bean(this.utils, "handleError")
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.status}"))
                .end()
                .removeHeaders("*")
                .marshal().json();
        // @formatter:on
    }

}
