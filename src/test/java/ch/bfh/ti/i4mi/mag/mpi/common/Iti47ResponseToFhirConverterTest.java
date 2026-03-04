package ch.bfh.ti.i4mi.mag.mpi.common;

import ch.bfh.ti.i4mi.mag.config.props.MagMpiProps;
import ch.bfh.ti.i4mi.mag.mhd.SchemeMapper;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Iti47ResponseToFhirConverterTest {

    private final Iti47ResponseToFhirConverter converter;

    Iti47ResponseToFhirConverterTest() {
        final var mpiProps = new MagMpiProps();
        mpiProps.setChEprspidAsPatientId(true);
        mpiProps.setChPdqmConstraints(false);
        mpiProps.setChPixmConstraints(false);

        final var schemeMapper = new SchemeMapper();

        this.converter = new Iti47ResponseToFhirConverter(schemeMapper, mpiProps);
    }

    @Test
    @DisplayName("The invalid ITI-47 response for QATestABG can be converted to a FHIR resource")
    void testIti47QaTestAbgResponse() throws Exception {
        // Arrange
        final var responseBytes = getClass().getClassLoader()
                .getResourceAsStream("iti47/qatestabg_response.xml")
                .readAllBytes();

        // Act
        final var patients = this.converter.convertForIti78(responseBytes);

        // Assert
        assertNotNull(patients);
        assertEquals(1, patients.size());
        assertInstanceOf(Patient.class, patients.getFirst());

        final var patient = (Patient) patients.getFirst();
        assertEquals("761337618580834093", patient.getId());
        assertTrue(patient.getActive());
        assertEquals(54, patient.getIdentifier().size());
        assertTrue(hasIdentifier(patient, "urn:oid:2.16.756.5.30.1.177.2.2.2.1", "100013169"));
        assertTrue(hasIdentifier(patient, "urn:oid:2.16.756.5.30.1.127.3.10.3", "761337618580834093"));
        assertTrue(hasIdentifier(patient, "urn:oid:2.16.756.5.32", "7563797564085"));
        assertEquals("QATestABG QAPersonABG", patient.getNameFirstRep().getNameAsSingleString());
        assertEquals(Enumerations.AdministrativeGender.MALE, patient.getGender());
        assertEquals("1910-12-31", patient.getBirthDateElement().getValueAsString());
        assertFalse(patient.hasTelecom());
        assertTrue(patient.hasAddress());
        assertEquals(1, patient.getAddress().size());

        final var address = patient.getAddressFirstRep();
        assertEquals("ZZ", address.getCountry());
        assertFalse(address.hasLine());
        assertFalse(address.hasCity());
        assertFalse(address.hasDistrict());
        assertFalse(address.hasState());
        assertFalse(address.hasPostalCode());
    }

    private boolean hasIdentifier(final Patient patient, final String system, final String value) {
        return patient.getIdentifier().stream()
                .anyMatch(id -> value.equals(id.getValue()) && system.equals(id.getSystem()));
    }
}