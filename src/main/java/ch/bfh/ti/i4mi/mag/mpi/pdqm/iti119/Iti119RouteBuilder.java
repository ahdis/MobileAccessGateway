package ch.bfh.ti.i4mi.mag.mpi.pdqm.iti119;

import ch.bfh.ti.i4mi.mag.common.PatientIdInterceptor;
import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import ch.bfh.ti.i4mi.mag.config.props.MagMpiProps;
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
public class Iti119RouteBuilder extends RouteBuilder {

    private final MagMpiProps mpiProps;
    private final Iti119ResponseConverter responseConverter;

    public Iti119RouteBuilder(final MagMpiProps mpiProps,
                              final Iti119ResponseConverter responseConverter) {
        super();
        this.mpiProps = mpiProps;
        this.responseConverter = responseConverter;
        log.debug("Iti119RouteBuilder initialized");
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti119RouteBuilder configure");

        final String xds47Endpoint = String.format(
                "pdqv3-iti47://%s?secure=%s",
                this.mpiProps.getIti47(),
                this.mpiProps.isHttps() ? "true" : "false"
        ) +
                //"&sslContextParameters=#pixContext" +
                "&audit=true" +
                "&auditContext=#auditContext" +
                "&inInterceptors=#soapResponseLogger" +
                "&inFaultInterceptors=#soapResponseLogger" +
                "&outInterceptors=#soapRequestLogger" +
                "&outFaultInterceptors=#soapRequestLogger";

        from("pdqm-iti119:translation?audit=true&auditContext=#auditContext").routeId("iti119-match")
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
