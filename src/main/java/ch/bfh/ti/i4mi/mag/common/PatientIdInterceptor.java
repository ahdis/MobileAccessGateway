package ch.bfh.ti.i4mi.mag.common;

import ch.bfh.ti.i4mi.mag.config.props.MagMpiProps;
import org.apache.camel.Message;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static ch.bfh.ti.i4mi.mag.MagConstants.EPR_SPID_OID;

/**
 * A response interceptor that can extract patient identifiers (XAD-PID and EPR-SPID), and inject them in the cache.
 **/
@Service
public class PatientIdInterceptor {
    private static final Logger log = LoggerFactory.getLogger(PatientIdInterceptor.class);

    private final PatientIdMappingService patientIdMappingService;
    private final String xadMpiOid;

    public PatientIdInterceptor(final PatientIdMappingService patientIdMappingService,
                                final MagMpiProps mpiProps) {
        this.patientIdMappingService = patientIdMappingService;
        this.xadMpiOid = mpiProps.getOids().getMpiPid();
    }

    @SuppressWarnings("unchecked")
    public void interceptBundleOfPatients(final Message message) {
        log.trace("interceptBundleOfPatients: Intercepting message with body of type {}", message.getBody().getClass().getName());
        final var bundle = message.getBody(Bundle.class);
        if (bundle != null) {
            bundle.getEntry().stream()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .filter(Objects::nonNull)
                    .filter(Patient.class::isInstance)
                    .map(Patient.class::cast)
                    .forEach(patient -> this.interceptIdentifiers(patient.getIdentifier()));
            return;
        }

        final var patientList = (List<Patient>) message.getBody(List.class);
        if (patientList != null) {
            patientList.forEach(patient -> this.interceptIdentifiers(patient.getIdentifier()));
            return;
        }

        log.warn("interceptBundleOfPatients: Unable to read a list of patients or a Bundle instance");
    }

    public void interceptIti83Parameters(final Message message) {
        log.trace("interceptIti83Parameters: Intercepting message with body of type {}", message.getBody().getClass().getName());
        final var parameters = message.getBody(Parameters.class);
        if (parameters == null) {
            log.warn("interceptIti83Parameters: Unable to read a Parameters instance");
            return;
        }

        final var identifierParams = parameters.getParameters("targetIdentifier");
        this.interceptIdentifiers(
                identifierParams.stream()
                        .map(Parameters.ParametersParameterComponent::getValue)
                        .filter(Identifier.class::isInstance)
                        .map(Identifier.class::cast)
                        .toList()
        );
    }

    public void interceptIdentifiers(final List<Identifier> identifiers) {
        log.trace("interceptIdentifiers: Intercepting list of identifiers with size {}", identifiers.size());
        String eprSpid = null;
        String xadPid = null;
        for (final var identifier : identifiers) {
            if (("urn:oid:" + EPR_SPID_OID).equals(identifier.getSystem())) {
                eprSpid = identifier.getValue();
            } else if (("urn:oid:" + this.xadMpiOid).equals(identifier.getSystem())) {
                xadPid = identifier.getValue();
            }
        }

        if (eprSpid != null && xadPid != null) {
            this.patientIdMappingService.save(xadPid, eprSpid);
        }
    }
}
