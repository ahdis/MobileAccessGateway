package ch.bfh.ti.i4mi.mag.pmir.iti119;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.pmir.iti78.Iti78RequestConverter;
import net.ihe.gazelle.hl7v3.datatypes.II;
import net.ihe.gazelle.hl7v3.datatypes.IVLTS;
import net.ihe.gazelle.hl7v3.prpain201305UV02.PRPAIN201305UV02Type;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02LivingSubjectId;
import org.apache.camel.Body;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.openehealth.ipf.commons.ihe.fhir.iti119.PdqmMatchInputPatient;

import javax.xml.bind.JAXBException;
import java.util.Optional;

public class Iti119RequestConverter extends Iti78RequestConverter {

    public Iti119RequestConverter(final Config config) {
        this.config = config;
    }

    public PRPAIN201305UV02Type convert(@Body final Parameters parameters) throws JAXBException {
        // Extract the Patient resource from the Parameters
        final var patient = Optional.ofNullable(parameters.getParameter("resource"))
                .map(Parameters.ParametersParameterComponent::getResource)
                .filter(Patient.class::isInstance)
                .map(Patient.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("Parameters must contain a Patient resource"));

        // Create an ITI-47 request
        final PRPAIN201305UV02Type request = initiateIti47Request(config.getPixMySenderOid(),
                                                                  config.getPixReceiverOid());
        final var parameterList = request.getControlActProcess().getQueryByParameter().getParameterList();

        // Map the search parameters from the Patient resource to the request

        // 1. Identifiers
        for (final var identifier : patient.getIdentifier()) {
            final var id = new PRPAMT201306UV02LivingSubjectId();
            id.addValue(new II(identifier.getSystem(), identifier.getValue()));
            id.setSemanticsText(ST("LivingSubject.id"));
            parameterList.addLivingSubjectId(id);
        }

        // 2. Name & Birth name
        for (final var name : patient.getName()) {
            parameterList.addLivingSubjectName(createLivingSubjectName(transform(name)));
        }

        // 3. Gender
        if (patient.hasGender()) {
            parameterList.addLivingSubjectAdministrativeGender(createAdministrativeGender(patient.getGender().toCode()));
        }

        // 4. Birth date
        if (patient.hasBirthDate()) {
            final var birthDate = new IVLTS();
            birthDate.setValue(patient.getBirthDateElement().getValueAsString().replace("-",""));
            parameterList.addLivingSubjectBirthTime(createBirthTime(birthDate));
        }

        // 5. Address
        for (final var address : patient.getAddress()) {
            parameterList.addPatientAddress(createAddress(transform(address)));
        }

        // 6. Maiden name (not required)
        if (patient.hasExtension(PdqmMatchInputPatient.MOTHERS_MAIDEN_NAME_EXT)) {
            final var ext = patient.getExtensionByUrl(PdqmMatchInputPatient.MOTHERS_MAIDEN_NAME_EXT);
            if (ext.getValue() instanceof StringType) {
                final var value = (StringType) ext.getValue();
                parameterList.addMothersMaidenName(createMothersMaidenName(value.getValue()));
            }
        }

        return request;
    }
}
