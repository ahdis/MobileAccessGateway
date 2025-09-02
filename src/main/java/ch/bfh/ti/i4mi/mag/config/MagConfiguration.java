package ch.bfh.ti.i4mi.mag.config;

import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IServerAddressStrategy;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.RestfulServerConfiguration;
import ch.bfh.ti.i4mi.mag.common.TcpSyslogSender;
import ch.bfh.ti.i4mi.mag.config.props.MagAuthProps;
import ch.bfh.ti.i4mi.mag.config.props.MagClientSslProps;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import ch.bfh.ti.i4mi.mag.fhir.MagCapabilityStatementProvider;
import jakarta.servlet.Filter;
import org.openehealth.ipf.commons.audit.protocol.AuditTransmissionProtocol;
import org.openehealth.ipf.commons.ihe.ws.cxf.payload.InPayloadLoggerInterceptor;
import org.openehealth.ipf.commons.ihe.ws.cxf.payload.OutPayloadLoggerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.Arrays;

@Configuration
public class MagConfiguration {

    @Bean(name = "stsEndpoint")
    public String getStsEndpoint(final MagAuthProps authProps,
                                 final MagClientSslProps clientSslProps) {
        return String.format("cxf://%s?dataFormat=CXF_MESSAGE&wsdlURL=%s&loggingFeatureEnabled=true" +
                                     "&inInterceptors=#soapResponseLogger" +
                                     "&inFaultInterceptors=#soapResponseLogger" +
                                     "&outInterceptors=#soapRequestLogger" +
                                     "&outFaultInterceptors=#soapRequestLogger" +
                                     (clientSslProps.isEnabled() ? "&sslContextParameters=#wsTlsContext" : ""),
                             authProps.getSts(), authProps.getStsWsdl());
    }

    @Bean
    public MagCapabilityStatementProvider serverConformanceProvider(final RestfulServer fhirServer) {
        return new MagCapabilityStatementProvider(fhirServer);
    }

    @Bean
    public IServerAddressStrategy serverAddressStrategy(final MagProps magProps) {
        // This sets the server address to the configured `mag.baseurl` value.
        // Without that bean, IpfFhirAutoConfiguration will use the ApacheProxyAddressStrategy instead.
        return new HardcodedServerAddressStrategy(magProps.getFhirBaseUrl());
    }

    @Bean
    @ConditionalOnMissingBean(name = "corsFilterRegistration")
    @ConditionalOnWebApplication
    public FilterRegistrationBean<Filter> corsFilterRegistration() {
        var frb = new FilterRegistrationBean<>();
        // Overwirte cors, otherwise we cannot access /camel/ via javascript
        // need to crosscheck with ch.bfh.ti.i4mi.mag.xuaSamlIDPIntegration
        frb.addUrlPatterns("/fhir/*", "/camel/*");
        frb.setFilter(new CorsFilter(request -> defaultCorsConfiguration()));
        return frb;
    }

    // use to fix https://github.com/i4mi/MobileAccessGateway/issues/56, however we have the CapabilityStatement not filled out anymore
    @Bean
    public RestfulServerConfiguration serverConfiguration(final IServerAddressStrategy serverAddressStrategy) {
        RestfulServerConfiguration config = new RestfulServerConfiguration();
        config.setResourceBindings(new ArrayList<>());
        config.setServerBindings(new ArrayList<>());
        config.setServerAddressStrategy(serverAddressStrategy);
        return config;
    }

    @Bean
    @ConditionalOnProperty(
            value = "ipf.atna.audit-repository-transport",
            havingValue = "TCP",
            matchIfMissing = false)
    public AuditTransmissionProtocol auditTransmissionProtocolTcp() {
        return new TcpSyslogSender();
    }

    // ---------------------------------------------
    // Logging configuration
    // ---------------------------------------------

    // see https://oehf.github.io/ipf-docs/docs/ihe/wsPayloadLogging
    @Bean
    public OutPayloadLoggerInterceptor soapRequestLogger() {
        return new OutPayloadLoggerInterceptor("./logs/[date('yyyyMMdd-HH00')]/[sequenceId]-soap-request.txt");
    }

    @Bean
    public InPayloadLoggerInterceptor soapResponseLogger() {
        return new InPayloadLoggerInterceptor("./logs/[date('yyyyMMdd-HH00')]/[sequenceId]-soap-response.txt");
    }


    private static CorsConfiguration defaultCorsConfiguration() {
        var cors = new CorsConfiguration();
        cors.addAllowedOrigin("*");
        cors.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // A comma separated list of allowed headers when making a non simple CORS request.
        cors.setAllowedHeaders(Arrays.asList("Origin",
                                             "Accept",
                                             "Content-Type",
                                             "Access-Control-Request-Method",
                                             "Access-Control-Request-Headers",
                                             "Authorization",
                                             "Prefer",
                                             "If-Match",
                                             "If-None-Match",
                                             "If-Modified-Since",
                                             "If-None-Exist",
                                             "Scope"));
        cors.setExposedHeaders(Arrays.asList("Location", "Content-Location", "ETag", "Last-Modified"));
        cors.setMaxAge(300L);
        return cors;
    }
}
