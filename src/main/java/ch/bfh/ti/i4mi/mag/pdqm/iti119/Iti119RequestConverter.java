package ch.bfh.ti.i4mi.mag.pdqm.iti119;

import ch.bfh.ti.i4mi.mag.config.props.MagMpiProps;
import ch.bfh.ti.i4mi.mag.mhd.SchemeMapper;
import ch.bfh.ti.i4mi.mag.pdqm.iti78.Iti78RequestConverter;
import jakarta.xml.bind.JAXBException;
import net.ihe.gazelle.hl7v3.datatypes.IVLTS;
import net.ihe.gazelle.hl7v3.prpain201305UV02.PRPAIN201305UV02Type;
import org.apache.camel.Body;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.openehealth.ipf.platform.camel.ihe.hl7v3.core.converters.JaxbHl7v3Converters;

import static ch.bfh.ti.i4mi.mag.MagConstants.FhirExtensionUrls.MOTHERS_MAIDEN_NAME;

public class Iti119RequestConverter extends Iti78RequestConverter {

    public Iti119RequestConverter(final SchemeMapper schemeMapper,
                                  final MagMpiProps magMpiProps) {
        super(schemeMapper, magMpiProps);
    }

    public String convert(@Body final Resource resource) throws JAXBException {
        if (resource instanceof final Patient patient) {
            return this.doConvert(patient);
        } else if (resource instanceof final Parameters params) {
            final var param = params.getParameter("resource");
            if (param != null && param.getResource() instanceof final Patient patient) {
                return this.doConvert(patient);
            }
        }
        throw new IllegalArgumentException(
                "Expected a Patient resource, or Parameters resource with a 'resource' parameter of type Patient");
    }

    private String doConvert(final Patient patient) throws JAXBException {
        // Create an ITI-47 request
        final PRPAIN201305UV02Type request = initiateIti47Request(this.mpiOidsProps.getSender(),
                                                                  this.mpiOidsProps.getReceiver());
        final var parameterList = request.getControlActProcess().getQueryByParameter().getParameterList();

        // Map the search parameters from the Patient resource to the request

        // 1. Identifiers
        for (final var identifier : patient.getIdentifier()) {
            parameterList.addLivingSubjectId(this.createSubjectId(identifier.getSystem(), identifier.getValue()));
        }

        // 2. Name & Birth name
        for (final var name : patient.getName()) {
            parameterList.addLivingSubjectName(this.createLivingSubjectName(transform(name)));
        }

        // 3. Gender
        if (patient.hasGender()) {
            parameterList.addLivingSubjectAdministrativeGender(this.createAdministrativeGender(patient.getGender().toCode()));
        }

        // 4. Birth date
        if (patient.hasBirthDate()) {
            final var birthDate = new IVLTS();
            birthDate.setValue(patient.getBirthDateElement().getValueAsString().replace("-", ""));
            parameterList.addLivingSubjectBirthTime(this.createBirthTime(birthDate));
        }

        // 5. Address
        for (final var address : patient.getAddress()) {
            parameterList.addPatientAddress(this.createAddress(transform(address)));
        }

        // 6. Maiden name (not required)
        if (patient.hasExtension(MOTHERS_MAIDEN_NAME)) {
            final var ext = patient.getExtensionByUrl(MOTHERS_MAIDEN_NAME);
            if (ext.getValue() instanceof final StringType extValue && !extValue.isEmpty()) {
                parameterList.addMothersMaidenName(this.createMothersMaidenName(extValue.getValue()));
            }
        }

        return JaxbHl7v3Converters.PRPAIN201305UV02toXml(request);
    }
}
