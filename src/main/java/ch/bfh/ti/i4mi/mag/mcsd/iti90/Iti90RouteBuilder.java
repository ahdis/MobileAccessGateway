package ch.bfh.ti.i4mi.mag.mcsd.iti90;

import ch.bfh.ti.i4mi.mag.common.MagRouteBuilder;
import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import ch.bfh.ti.i4mi.mag.config.props.MagHpdProps;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("mag.hpd.iti-58")
public class Iti90RouteBuilder extends MagRouteBuilder {
    private static final Logger log = LoggerFactory.getLogger(Iti90RouteBuilder.class);

    private final MagHpdProps hpdProps;

    public Iti90RouteBuilder(final MagProps magProps) {
        super(magProps);
        this.hpdProps = magProps.getHpd();
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti90RouteBuilder configure");

        final String iti58Endpoint = this.buildOutgoingEndpoint("hpd-iti58",
                                                                this.hpdProps.getIti58(),
                                                                this.hpdProps.isHttps());

        from("mcsd-iti90:find-matching-care-services?audit=false")
                .routeId("in-mcsd-iti90")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                .process(RequestHeadersForwarder.checkAuthorization(true))
                .process(RequestHeadersForwarder.forward())
                .process(Utils.keepBody())
                .doTry()
                    .bean(Iti90RequestConverter.class, "convert")
                    .to(iti58Endpoint)
                    .process(Utils.keptBodyToHeader())
                    .process(TraceparentHandler.updateHeaderForFhir())
                    .bean(Iti90ResponseConverter.class, "convert")
                .doCatch(Exception.class)
                    .setBody(simple("${exception}"))
                    .process(this.errorFromException())
                .end();

    }
}
