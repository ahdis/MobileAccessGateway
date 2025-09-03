package ch.bfh.ti.i4mi.mag.sts;

import ch.bfh.ti.i4mi.mag.config.props.MagAuthProps;
import ch.bfh.ti.i4mi.mag.config.props.MagClientSslProps;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class DirectStsRouteBuilder extends RouteBuilder {

    private final StsUtils utils;
    private final MagAuthProps authProps;
    private final boolean clientSsl;

    public DirectStsRouteBuilder(final StsUtils utils,
                                 final MagAuthProps authProps,
                                 final MagClientSslProps clientSslProps) {
        this.utils = utils;
        this.authProps = authProps;
        this.clientSsl = clientSslProps.isEnabled();
    }

    @Override
    public void configure() throws Exception {
        final String assertionEndpoint = String.format(
                "cxf://%s?dataFormat=CXF_MESSAGE&wsdlURL=%s&loggingFeatureEnabled=true" +
                        "&inInterceptors=#soapResponseLogger" +
                        "&inFaultInterceptors=#soapResponseLogger" +
                        "&outInterceptors=#soapRequestLogger" +
                        "&outFaultInterceptors=#soapRequestLogger" +
                        (clientSsl ? "&sslContextParameters=#wsTlsContext" : ""),
                this.authProps.getSts(),
                this.authProps.getStsWsdl());

        from("direct:sts")
                .routeId("internal-sts")
                /*.setHeader(CxfConstants.OPERATION_NAME,
                           constant("Issue"))
                .setHeader(CxfConstants.OPERATION_NAMESPACE,
                           constant("http://docs.oasis-open.org/ws-sx/ws-trust/200512/wsdl"))*/
                .to(assertionEndpoint)
                .bean(this.utils, "extractAssertionAsString");
    }
}
