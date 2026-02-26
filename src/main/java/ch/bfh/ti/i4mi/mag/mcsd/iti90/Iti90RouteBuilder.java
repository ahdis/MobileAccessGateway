package ch.bfh.ti.i4mi.mag.mcsd.iti90;

import ch.bfh.ti.i4mi.mag.common.MagRouteBuilder;
import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import ch.bfh.ti.i4mi.mag.config.props.MagHpdProps;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import org.apache.camel.LoggingLevel;
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
        log.debug("Configuring ITI-90 route");

        final String iti58Endpoint = this.buildOutgoingEndpoint("hpd-iti58",
                                                                this.hpdProps.getIti58(),
                                                                this.hpdProps.isHttps());

        // @formatter:off
        from("mcsd-iti90:find-matching-care-services?audit=false")
                .routeId("in-mcsd-iti90")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                .log(LoggingLevel.INFO, log, "Received ITI-90 request")
                .process(loggingRequestProcessor(LoggingLevel.TRACE, log))
                .process(RequestHeadersForwarder.checkAuthorization(true))
                .process(RequestHeadersForwarder.forward())
                .process(Utils.keepBody())
                .doTry()
                    .bean(Iti90RequestConverter.class, "convert")
                    .log(LoggingLevel.DEBUG, log, "Sending an ITI-58 request to " + iti58Endpoint)
                    .log(LoggingLevel.TRACE, log, "${body}")
                    .to(iti58Endpoint)
                    .log(LoggingLevel.DEBUG, log, "Got a response")
                    .log(LoggingLevel.TRACE, log, "${body}")
                    .process(Utils.keptBodyToHeader())
                    .process(TraceparentHandler.updateHeaderForFhir())
                    .bean(Iti90ResponseConverter.class, "convert")
                    .log(LoggingLevel.DEBUG, log, "Finished generating the ITI-90 response")
                    .process(loggingResponseProcessor(LoggingLevel.TRACE, log))
                .doCatch(Exception.class)
                    .setBody(simple("${exception}"))
                    .process(this.errorFromException())
                .end();
        // @formatter:on
    }
}
