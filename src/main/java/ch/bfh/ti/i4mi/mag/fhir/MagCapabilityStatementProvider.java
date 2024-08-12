package ch.bfh.ti.i4mi.mag.fhir;

import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ch.bfh.ti.i4mi.mag.xua.Iti71RouteBuilder;
import ch.bfh.ti.i4mi.mag.xua.TokenEndpointRouteBuilder;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.BooleanType;
import org.openehealth.ipf.commons.ihe.fhir.support.NullsafeServerCapabilityStatementProvider;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * A customized provider of the server CapabilityStatement that adds the OAuth URIs extension
 * and no-paging configuration.
 *
 * @author Quentin Ligier
 * @author Luis Filipe de Sousa
 */
@Interceptor
public class MagCapabilityStatementProvider extends NullsafeServerCapabilityStatementProvider {
    private static final Logger logger = LoggerFactory.getLogger(MagCapabilityStatementProvider.class);
    private final String baseUrl;

    public MagCapabilityStatementProvider(final RestfulServer fhirServer,
                                          @Value("${mag.baseurl}") final String baseUrl) {
        super(fhirServer);
        fhirServer.setServerConformanceProvider(this);
        this.baseUrl = baseUrl + "/camel/";
    }

    @Override
    public IBaseConformance getServerConformance(HttpServletRequest theRequest, RequestDetails theRequestDetails) {
        logger.info("Generating CapabilityStatement with OAuth URIs and no-paging configuration");
        final var conformance = (CapabilityStatement) super.getServerConformance(theRequest, theRequestDetails);

        // Add OAuth URIs extension
        conformance.getRestFirstRep().getSecurity().addExtension()
                .setUrl("http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris")
                .addExtension(new Extension("token",
                        new StringType(this.baseUrl + TokenEndpointRouteBuilder.TOKEN_PATH)))
                .addExtension(new Extension("authorize",
                        new StringType(this.baseUrl + Iti71RouteBuilder.AUTHORIZE_PATH)));

        // Add no-paging configuration
        // CapabilityStatement.CapabilityStatementRestComponent rest = conformance.getRestFirstRep();
        // Extension noPagingExtension = new Extension("http://example.com/fhir/StructureDefinition/no-paging");
        // noPagingExtension.setValue(new BooleanType(true));
        // rest.addExtension(noPagingExtension);

        // Remove any existing paging-related search parameters
        // rest.getResource().forEach(resource ->
        //        resource.getSearchParam().removeIf(param -> param.getName().equals("_count")));

        logger.debug("Updated CapabilityStatement: {}", conformance);

        return conformance;
    }
}