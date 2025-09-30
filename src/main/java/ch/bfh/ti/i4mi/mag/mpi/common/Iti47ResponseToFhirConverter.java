/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.bfh.ti.i4mi.mag.mpi.common;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.common.JavaUtils;
import ch.bfh.ti.i4mi.mag.config.props.MagMpiProps;
import ch.bfh.ti.i4mi.mag.mhd.SchemeMapper;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBException;
import net.ihe.gazelle.hl7v3.coctmt030007UV.COCTMT030007UVPerson;
import net.ihe.gazelle.hl7v3.datatypes.AD;
import net.ihe.gazelle.hl7v3.datatypes.BL;
import net.ihe.gazelle.hl7v3.datatypes.CE;
import net.ihe.gazelle.hl7v3.datatypes.CS;
import net.ihe.gazelle.hl7v3.datatypes.EN;
import net.ihe.gazelle.hl7v3.datatypes.INT;
import net.ihe.gazelle.hl7v3.datatypes.PN;
import net.ihe.gazelle.hl7v3.datatypes.TEL;
import net.ihe.gazelle.hl7v3.datatypes.TS;
import net.ihe.gazelle.hl7v3.prpain201306UV02.PRPAIN201306UV02MFMIMT700711UV01ControlActProcess;
import net.ihe.gazelle.hl7v3.prpain201306UV02.PRPAIN201306UV02MFMIMT700711UV01Subject1;
import net.ihe.gazelle.hl7v3.prpain201306UV02.PRPAIN201306UV02Type;
import net.ihe.gazelle.hl7v3.prpamt201310UV02.PRPAMT201310UV02LanguageCommunication;
import net.ihe.gazelle.hl7v3.prpamt201310UV02.PRPAMT201310UV02Patient;
import net.ihe.gazelle.hl7v3.prpamt201310UV02.PRPAMT201310UV02Person;
import net.ihe.gazelle.hl7v3.prpamt201310UV02.PRPAMT201310UV02PersonalRelationship;
import net.ihe.gazelle.hl7v3.prpamt201310UV02.PRPAMT201310UV02QueryMatchObservation;
import net.ihe.gazelle.hl7v3.prpamt201310UV02.PRPAMT201310UV02Subject;
import net.ihe.gazelle.hl7v3transformer.HL7V3Transformer;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.PatientCommunicationComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.codesystems.MatchGrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static ca.uhn.fhir.model.api.ResourceMetadataKeyEnum.ENTRY_SEARCH_MODE;
import static ca.uhn.fhir.model.api.ResourceMetadataKeyEnum.ENTRY_SEARCH_SCORE;
import static ca.uhn.fhir.model.valueset.BundleEntrySearchModeEnum.MATCH;
import static ch.bfh.ti.i4mi.mag.MagConstants.EPR_SPID_OID;
import static ch.bfh.ti.i4mi.mag.MagConstants.FhirExtensionUrls.MOTHERS_MAIDEN_NAME;
import static ch.bfh.ti.i4mi.mag.mpi.common.Hl7v3Mappers.transform;
import static ch.bfh.ti.i4mi.mag.mpi.common.Hl7v3Mappers.val;
import static ch.bfh.ti.i4mi.mag.mpi.common.Hl7v3Mappers.verifyAck;
import static org.openehealth.ipf.commons.ihe.fhir.iti119.AdditionalResourceMetadataKeyEnum.ENTRY_MATCH_GRADE;

/**
 * Convert an ITI-47 response back to a Bundle of Patient resources.
 *
 * @author alexander kreutz
 */
@Component
public class Iti47ResponseToFhirConverter {
    private static final Logger log = LoggerFactory.getLogger(Iti47ResponseToFhirConverter.class);

    private final SchemeMapper schemeMapper;
    private final MagMpiProps mpiProps;

    public Iti47ResponseToFhirConverter(final SchemeMapper schemeMapper, final MagMpiProps mpiProps) {
        this.schemeMapper = schemeMapper;
        this.mpiProps = mpiProps;
    }

    @SuppressWarnings("unused")
    public List<Resource> convertForIti119(final byte[] input) {
        return this.convertAndCatch(input, false);
    }

    @SuppressWarnings("unused")
    public List<Resource> convertForIti78(final byte[] input) {
        return this.convertAndCatch(input, true);
    }

    @SuppressWarnings("unused")
    public List<Resource> convertForIti104(final byte[] input) {
        return this.convertAndCatch(input, false);
    }

    private List<Resource> convertAndCatch(final byte[] input, final boolean verifyAck) {
        try {
            return this.convert(input, verifyAck);
        } catch (final BaseServerResponseException controlledException) {
            log.debug("ITI-47 response converter: caught an HAPI exception", controlledException);
            throw controlledException;
        } catch (final JAXBException parsingException) {
            log.debug("ITI-47 response converter: caught a JAXB exception", parsingException);
            throw new InvalidRequestException("Failed parsing ITI-47 response", parsingException);
        } catch (final Exception otherException) {
            log.debug("ITI-47 response converter: caught an exception", otherException);
            throw new InvalidRequestException("Unexpected exception during ITI-47 response processing", otherException);
        }
    }

    private List<Resource> convert(final byte[] input, final boolean verifyAck) throws Exception {
        final PRPAIN201306UV02Type pdqResponse = HL7V3Transformer.unmarshallMessage(PRPAIN201306UV02Type.class,
                                                                                    new ByteArrayInputStream(input));
        final PRPAIN201306UV02MFMIMT700711UV01ControlActProcess controlAct = pdqResponse.getControlActProcess();

        if (verifyAck) {
            // OK NF AE
            verifyAck(controlAct.getQueryAck().getQueryResponseCode(), pdqResponse.getAcknowledgement(), "ITI-47");
        }

        final List<PRPAIN201306UV02MFMIMT700711UV01Subject1> subjects = controlAct.getSubject();
        if (this.mpiProps.isChPdqmConstraints() && subjects.size() > 5) {
            // https://github.com/i4mi/MobileAccessGateway/issues/171
            final var operationOutcome = new OperationOutcome();
            var code = new CodeableConcept();
            code.addCoding().setSystem("urn:oid:1.3.6.1.4.1.19376.1.2.27.1").setCode(
                    "LivingSubjectAdministrativeGenderRequested").setDisplay(
                    "LivingSubjectAdministrativeGenderRequested");
            operationOutcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.WARNING).setCode(OperationOutcome.IssueType.INCOMPLETE).setDetails(
                    code);

            code = new CodeableConcept();
            code.addCoding().setSystem("urn:oid:1.3.6.1.4.1.19376.1.2.27.1").setCode(
                    "LivingSubjectBirthPlaceNameRequested").setDisplay("LivingSubjectBirthPlaceNameRequested");
            operationOutcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.WARNING).setCode(OperationOutcome.IssueType.INCOMPLETE).setDetails(
                    code);

            code = new CodeableConcept();
            code.addCoding().setSystem("urn:oid:2.16.756.5.30.1.127.3.10.17").setCode("BirthNameRequested").setDisplay(
                    "BirthNameRequested");
            operationOutcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.WARNING).setCode(OperationOutcome.IssueType.INCOMPLETE).setDetails(
                    code);

            return List.of(operationOutcome);
        }

        final List<Resource> patients = new ArrayList<>(subjects.size());

        // Iterate over all subjects (patients) and convert them
        for (PRPAIN201306UV02MFMIMT700711UV01Subject1 subject : subjects) {
            final PRPAMT201310UV02Patient patient = subject.getRegistrationEvent().getSubject1().getPatient();
            final PRPAMT201310UV02Person patientPerson = patient.getPatientPerson();

            if (patient.getId().isEmpty()) {
                continue;
            }

            final Patient result = new Patient();

            Stream.concat(patient.getId().stream(),
                          patientPerson.getAsOtherIDs().stream().flatMap(oi -> oi.getId().stream()))
                    .filter(ii -> ii.getRoot() != null && ii.getExtension() != null)
                    .forEach(patientId -> {
                        if (this.mpiProps.isChPdqmConstraints()) {
                            if (this.mpiProps.getOids().getMpiPid().equals(patientId.getRoot()) || EPR_SPID_OID.equals(patientId.getRoot())) {
                                result.addIdentifier(this.schemeMapper.toFhirIdentifier(patientId.getRoot(),
                                                                                        patientId.getExtension(),
                                                                                        null));
                            } else {
                                log.debug("Ignoring patient identifier " + patientId.getRoot());
                            }
                        } else {
                            result.addIdentifier(this.schemeMapper.toFhirIdentifier(patientId.getRoot(),
                                                                                    patientId.getExtension(),
                                                                                    null));
                        }

                        if (this.mpiProps.isChEprspidAsPatientId() && EPR_SPID_OID.equals(patientId.getRoot())) {
                            result.setId(patientId.getExtension());
                        }
                    });

            // Generate an ID if it's missing
            if (!result.hasId()) {
                result.setId(UUID.randomUUID().toString());
            }

            CS statusCode = patient.getStatusCode();
            if (statusCode != null && "active".equals(statusCode.getCode())) result.setActive(true);

            for (PN name : patientPerson.getName()) {
                result.addName(transform(name));
            }

            CE gender = patientPerson.getAdministrativeGenderCode();
            if (gender != null) {
                switch (gender.getCode()) {
                    case "M":
                        result.setGender(AdministrativeGender.MALE);
                        break;
                    case "F":
                        result.setGender(AdministrativeGender.FEMALE);
                        break;
                    case "A":
                        result.setGender(AdministrativeGender.OTHER);
                        break;
                    case "U":
                        result.setGender(AdministrativeGender.UNKNOWN);
                        break;
                }
            }
            TS birthTime = patientPerson.getBirthTime();
            if (birthTime != null) {
                result.setBirthDateElement(transform(birthTime));
            }
            for (AD ad : patientPerson.getAddr()) {
                result.addAddress(transform(ad));
            }
            for (TEL tel : patientPerson.getTelecom()) {
                result.addTelecom(transform(tel));
            }
            for (PRPAMT201310UV02LanguageCommunication lang : patientPerson.getLanguageCommunication()) {
                CE langCode = lang.getLanguageCode();
                PatientCommunicationComponent pcc = new PatientCommunicationComponent();
                pcc.setLanguage(this.transformCe(langCode));
                BL preferred = lang.getPreferenceInd();
                if (preferred != null && preferred.getValue().booleanValue()) pcc.setPreferred(true);
                result.addCommunication(pcc);
            }

            TS deceasedTime = patientPerson.getDeceasedTime();
            if (deceasedTime != null) result.setDeceased(transform(deceasedTime));
            else {
                BL deceased = patientPerson.getDeceasedInd();
                if (deceased != null) result.setDeceased(new BooleanType(deceased.getValue().booleanValue()));
            }

            INT multiBirthOrder = patientPerson.getMultipleBirthOrderNumber();
            if (multiBirthOrder != null) {
                result.setMultipleBirth(new IntegerType(multiBirthOrder.getValue()));
            } else {
                BL multipleBirth = patientPerson.getMultipleBirthInd();
                if (multipleBirth != null)
                    result.setMultipleBirth(new BooleanType(multipleBirth.getValue().booleanValue()));
            }

            CE maritalStatus = patientPerson.getMaritalStatusCode();
            result.setMaritalStatus(this.transformCe(maritalStatus));

            for (PRPAMT201310UV02PersonalRelationship relationShip : patientPerson.getPersonalRelationship()) {
                CE code = relationShip.getCode();
                if (code != null && "MTH".equals(code.getCode()) && "2.16.840.1.113883.12.63".equals(code.getCodeSystem())) {
                    COCTMT030007UVPerson holder = relationShip.getRelationshipHolder1();
                    if (holder != null && !holder.getName().isEmpty()) {
                        EN name = holder.getName().getFirst();
                        if (!name.getFamily().isEmpty()) {
                            String familyName = val(name.getFamily());
                            result.addExtension(MOTHERS_MAIDEN_NAME, new StringType(familyName));
                        }
                    }
                }
            }

            final float score = Optional.ofNullable(patient.getSubjectOf1()).map(JavaUtils::firstOrNull).map(
                    PRPAMT201310UV02Subject::getQueryMatchObservation).map(PRPAMT201310UV02QueryMatchObservation::getValue).filter(
                    INT.class::isInstance).map(INT.class::cast).map(INT::getValue).map(i -> i / 100f).orElse(1.0f);
            final MatchGrade scoreCode;
            if (score >= 0.75) {
                scoreCode = MatchGrade.CERTAIN;
            } else if (score >= 0.5) {
                scoreCode = MatchGrade.PROBABLE;
            } else {
                scoreCode = MatchGrade.POSSIBLE;
            }

            // This is HAPI's way to store Bundle.entry.search information for resources that are returned directly.
            // I.e. we can't make the Bundle ourselves here, because the consumer expects a List<Resource>.
            ENTRY_SEARCH_MODE.put(result, MATCH);
            ENTRY_SEARCH_SCORE.put(result, BigDecimal.valueOf(score));
            ENTRY_MATCH_GRADE.put(result, scoreCode);

            patients.add(result);
        }

        return patients;
    }

    public OperationOutcome error(IssueType type, String diagnostics) {
        OperationOutcome result = new OperationOutcome();

        OperationOutcomeIssueComponent issue = result.addIssue();
        issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
        issue.setCode(type);
        issue.setDiagnostics(diagnostics);
        return result;
    }

    @Nullable
    public CodeableConcept transformCe(final @Nullable CE in) {
        if (in == null) {
            return null;
        }
        final var cc = new CodeableConcept();
        cc.addCoding(this.schemeMapper.toFhirCoding(in.getCode(), in.getCodeSystem()).setDisplay(in.getDisplayName()));
        return cc;
    }
}
