package ch.bfh.ti.i4mi.mag.mpi.pdqm.iti119;

import ch.bfh.ti.i4mi.mag.common.MagRouteBuilder;
import ch.bfh.ti.i4mi.mag.common.PatientIdInterceptor;
import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import ch.bfh.ti.i4mi.mag.config.props.MagMpiProps;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import ch.bfh.ti.i4mi.mag.mhd.BaseResponseConverter;
import jakarta.xml.ws.soap.SOAPFaultException;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

/**
 * IHE PDQm: ITI-119 Patient Demographics Match
 */
@Slf4j
@Component
@ConditionalOnProperty("mag.mpi.iti-47")
public class Iti119RouteBuilder extends MagRouteBuilder {

    private final MagMpiProps mpiProps;
    private final Iti119ResponseConverter responseConverter;

    public Iti119RouteBuilder(final MagProps magProps,
                              final Iti119ResponseConverter responseConverter) {
        super(magProps);
        this.mpiProps = magProps.getMpi();
        this.responseConverter = responseConverter;
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
                //.process(RequestHeadersForwarder.checkAuthorization(this.mpiProps.isChPdqmConstraints()))
                .process(RequestHeadersForwarder.forward())
                .bean(Iti119RequestConverter.class, "convert")
                .doTry()
                .to(xds47Endpoint)
                .process(TraceparentHandler.updateHeaderForFhir())
                .process(translateToFhir(this.responseConverter, byte[].class))
                .bean(PatientIdInterceptor.class, "interceptBundleOfPatients")
                .doCatch(SOAPFaultException.class)
                .setBody(simple("${exception}"))
                .bean(BaseResponseConverter.class, "errorFromException")
                .end();
    }
}
