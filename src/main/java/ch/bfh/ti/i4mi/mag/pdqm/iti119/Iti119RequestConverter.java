package ch.bfh.ti.i4mi.mag.pdqm.iti119;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.pdqm.iti78.Iti78RequestConverter;
import jakarta.xml.bind.JAXBException;
import net.ihe.gazelle.hl7v3.datatypes.II;
import net.ihe.gazelle.hl7v3.datatypes.IVLTS;
import net.ihe.gazelle.hl7v3.prpain201305UV02.PRPAIN201305UV02Type;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02LivingSubjectId;
import org.apache.camel.Body;
import org.openehealth.ipf.commons.ihe.fhir.iti119.PdqmMatchInputPatient;

import static ch.bfh.ti.i4mi.mag.MagConstants.FhirExtensionUrls.MOTHERS_MAIDEN_NAME;

public class Iti119RequestConverter extends Iti78RequestConverter {

    public Iti119RequestConverter(final Config config) {
        this.config = config;
    }

    public PRPAIN201305UV02Type convert(@Body final PdqmMatchInputPatient patient) throws JAXBException {
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
        if (patient.hasMothersMaidenName()) {
            parameterList.addMothersMaidenName(createMothersMaidenName(patient.getMothersMaidenName().getValue()));
        }

        return request;
    }
}
