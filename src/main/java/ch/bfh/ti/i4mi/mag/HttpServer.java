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

package ch.bfh.ti.i4mi.mag;

import org.eclipse.jetty.ee10.servlet.ErrorHandler;
import org.eclipse.jetty.server.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Offers an optional additional HTTP server if HTTPS is used
 * @author alexander kreutz
 */
@Configuration
public class HttpServer implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

    @Value("${server.http.port:0}")
    private int httpPort;
    
    @Value("${server.max-http-header-size:0}")
    private int maxHttpHeaderSize;

    @Override
    public void customize(final JettyServletWebServerFactory factory) {
        final JettyServerCustomizer customizer = server -> {
            final var errorHandler = new ErrorHandler();
            errorHandler.setShowStacks(false); // Disable stacktraces
            server.setErrorHandler(errorHandler);
        };
        factory.addServerCustomizers(customizer);
    }

    @Bean
    public WebServerFactoryCustomizer<JettyServletWebServerFactory> webServerFactoryCustomizer() {
        return factory -> {
            if (httpPort > 0) {
                factory.addServerCustomizers(server -> {
                    ServerConnector httpConnector = new ServerConnector(server);
                    httpConnector.setPort(httpPort);
                    server.addConnector(httpConnector);
                    if (maxHttpHeaderSize > 0) {
                        for (ConnectionFactory factory1 : httpConnector.getConnectionFactories()) {
                            if (factory1 instanceof final HttpConfiguration.ConnectionFactory cf) {
                                cf.getHttpConfiguration().setRequestHeaderSize(maxHttpHeaderSize);
                            }
                        }
                    }
                });
            }
        };
    }
}