package ch.bfh.ti.i4mi.mag.pmir.iti119;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import ch.bfh.ti.i4mi.mag.mhd.BaseResponseConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.xml.ws.soap.SOAPFaultException;

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

/**
 * IHE PDQm: ITI-119 Patient Demographics Match
 */
@Slf4j
@Component
@ConditionalOnProperty("mag.pix.iti-47.url")
public class Iti119RouteBuilder extends RouteBuilder {

    private final Config config;
    private final Iti119ResponseConverter responseConverter;

    public Iti119RouteBuilder(final Config config,
                              final Iti119ResponseConverter responseConverter) {
        super();
        this.config = config;
        this.responseConverter = responseConverter;
        log.debug("Iti119RouteBuilder initialized");
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti119RouteBuilder configure");

        final String xds47Endpoint = String.format(
                "pdqv3-iti47://%s?secure=%s", this.config.getIti47HostUrl(), this.config.isPixHttps() ? "true" : "false"
        ) +
                //"&sslContextParameters=#pixContext" +
                "&audit=true" +
                "&auditContext=#myAuditContext" +
                "&inInterceptors=#soapResponseLogger" +
                "&inFaultInterceptors=#soapResponseLogger"+
                "&outInterceptors=#soapRequestLogger" +
                "&outFaultInterceptors=#soapRequestLogger";

        from("pdqm-iti119:translation?audit=true&auditContext=#myAuditContext").routeId("pdqm-adapter")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                .process(RequestHeadersForwarder.checkAuthorization(config.isChPdqmConstraints()))
                .process(RequestHeadersForwarder.forward())
                .bean(Iti119RequestConverter.class, "convert")
                .doTry()
                    .to(xds47Endpoint)
                    .process(TraceparentHandler.updateHeaderForFhir())
                    .process(translateToFhir(responseConverter , byte[].class))
                .doCatch(SOAPFaultException.class)
                    .setBody(simple("${exception}"))
                    .bean(BaseResponseConverter.class, "errorFromException")
                .end();
    }
}
