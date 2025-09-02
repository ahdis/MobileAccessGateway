package ch.bfh.ti.i4mi.mag.common;

import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import static ch.bfh.ti.i4mi.mag.config.TlsConfiguration.BEAN_TLS_CONTEXT_WS;

public abstract class MagRouteBuilder extends RouteBuilder {

    protected final MagProps magProps;

    protected MagRouteBuilder(final MagProps magProps) {
        super();
        this.magProps = magProps;
    }

    protected String buildOutgoingEndpoint(final String scheme,
                                           final String uri,
                                           final boolean useTls) {
        final var builder = UriComponentsBuilder.fromUriString(uri)
                .scheme(scheme)
                .queryParam("secure", String.valueOf(useTls))
                .queryParam("audit", String.valueOf(this.magProps.getAtna().isAuditEnabled()));

        if (useTls) {
            builder.queryParam("sslContextParameters", "#" + BEAN_TLS_CONTEXT_WS);
        }

//        "&inInterceptors=#soapResponseLogger" +
//        "&inFaultInterceptors=#soapResponseLogger"+
//        "&outInterceptors=#soapRequestLogger" +
//        "&outFaultInterceptors=#soapRequestLogger";
        final var endpoint = builder.build().toUriString().replace(":/", "://");
        log.info("Built endpoint: {}", endpoint);
        return endpoint;
    }
}
