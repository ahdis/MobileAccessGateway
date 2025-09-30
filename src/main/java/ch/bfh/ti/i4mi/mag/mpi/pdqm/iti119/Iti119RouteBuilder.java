package ch.bfh.ti.i4mi.mag.mpi.pdqm.iti119;

import ch.bfh.ti.i4mi.mag.common.MagRouteBuilder;
import ch.bfh.ti.i4mi.mag.common.PatientIdInterceptor;
import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import ch.bfh.ti.i4mi.mag.config.props.MagMpiProps;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import ch.bfh.ti.i4mi.mag.mpi.common.Iti47ResponseToFhirConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * IHE PDQm: ITI-119 Patient Demographics Match
 */
@Component
@ConditionalOnProperty("mag.mpi.iti-47")
public class Iti119RouteBuilder extends MagRouteBuilder {
    private static final Logger log = LoggerFactory.getLogger(Iti119RouteBuilder.class);

    private final MagMpiProps mpiProps;

    public Iti119RouteBuilder(final MagProps magProps) {
        super(magProps);
        this.mpiProps = magProps.getMpi();
        log.debug("Iti119RouteBuilder initialized");
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti119RouteBuilder configure");

        final String xds47Endpoint = this.buildOutgoingEndpoint("pdqv3-iti47",
                                                                this.mpiProps.getIti47(),
                                                                this.mpiProps.isHttps());

        from("pdqm-iti119:patient-demographics-match?audit=false")
                .routeId("in-pdqm-iti119")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                .doTry()
                //.process(RequestHeadersForwarder.checkAuthorization(this.mpiProps.isChPdqmConstraints()))
                    .process(RequestHeadersForwarder.forward())
                    .bean(Iti119RequestConverter.class, "convert")
                    .to(xds47Endpoint)
                    .process(TraceparentHandler.updateHeaderForFhir())
                    .bean(Iti47ResponseToFhirConverter.class, "convertForIti119")
                    .bean(PatientIdInterceptor.class, "interceptBundleOfPatients")
                .doCatch(Exception.class)
                    .setBody(simple("${exception}"))
                    .process(this.errorFromException())
                .end();
    }
}
