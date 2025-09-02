package ch.bfh.ti.i4mi.mag.fhir;

import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.openehealth.ipf.commons.ihe.fhir.support.NullsafeServerCapabilityStatementProvider;

/**
 * A customized provider of the server CapabilityStatement that adds the OAuth URIs extension.
 *
 * @author Quentin Ligier
 */
@Interceptor
public class MagCapabilityStatementProvider extends NullsafeServerCapabilityStatementProvider {

    public MagCapabilityStatementProvider(final RestfulServer fhirServer) {
        super(fhirServer);
        fhirServer.setServerConformanceProvider(this);
    }

    @Override
    public IBaseConformance getServerConformance(HttpServletRequest theRequest, RequestDetails theRequestDetails) {
        final var conformance = (CapabilityStatement) super.getServerConformance(theRequest, theRequestDetails);

        // reduce [ "application/fhir+xml", "xml", "application/fhir+json", "json", "html/json", "html/xml" ],
        // see https://ehealthsuisse.ihe-europe.net/evs/default/validator.seam?standard=59
        // the last two come from ResponseHighlighterInterceptor, they should be valid?
        conformance.setName("MobileAccessGateway");
        conformance.setPublisher("ahdis ag");

        var resources = conformance.getRestFirstRep().getResource();
        for (final var resource : resources) {
            if (resource.getType().equals("Patient")) {
                for (final var op : resource.getOperation()) {
                    switch (op.getName()) {
                        case "ihe-pix" -> op.setDefinition("http://fhir.ch/ig/ch-epr-fhir/OperationDefinition/CH.PIXm");
                        case "match" -> op.setDefinition("http://fhir.ch/ig/ch-epr-fhir/OperationDefinition/CHPDQmMatch");
                        default -> {}
                    }
                }
            }
        }

        return conformance;
    }
}
