/*
 * Copyright 2024 the original author or authors.
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
package ch.bfh.ti.i4mi.mag;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;

abstract public class MagRouteBuilder extends RouteBuilder {

    protected final Config config;

    protected MagRouteBuilder(Config config) {
        this.config = config;
    }

    protected String createSoapEndpointUri(String schema, String partialUrl) {
        return schema + "://" + partialUrl +
                "?secure=" + config.isHttps() +
                "&audit=true" +
                "&auditContext=#myAuditContext" +
                "&inInterceptors=#soapResponseLogger" +
                "&inFaultInterceptors=#soapResponseLogger"+
                "&outInterceptors=#soapRequestLogger" +
                "&outFaultInterceptors=#soapRequestLogger";
    }

    protected RouteDefinition fromDirect(String name) {
        return from("direct:" + name).routeId(name);
    }

}
