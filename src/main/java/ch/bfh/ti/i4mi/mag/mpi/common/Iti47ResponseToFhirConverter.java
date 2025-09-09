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
import net.ihe.gazelle.hl7v3.datatypes.*;
import net.ihe.gazelle.hl7v3.prpain201306UV02.*;
import net.ihe.gazelle.hl7v3.prpamt201310UV02.*;
import net.ihe.gazelle.hl7v3transformer.HL7V3Transformer;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Patient.PatientCommunicationComponent;
import org.openehealth.ipf.commons.ihe.fhir.translation.ToFhirTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static ch.bfh.ti.i4mi.mag.MagConstants.EPR_SPID_OID;
import static ch.bfh.ti.i4mi.mag.MagConstants.FhirExtensionUrls.MOTHERS_MAIDEN_NAME;
import static ch.bfh.ti.i4mi.mag.mpi.common.Hl7v3Mappers.*;

/**
 * Convert an ITI-47 response back to a Bundle of Patient resources.
 *
 * @author alexander kreutz
 */
@Component
public class Iti47ResponseToFhirConverter implements ToFhirTranslator<byte[]> {
    private static final Logger log = LoggerFactory.getLogger(Iti47ResponseToFhirConverter.class);

    private final SchemeMapper schemeMapper;
    private final MagMpiProps mpiProps;

    public Iti47ResponseToFhirConverter(final SchemeMapper schemeMapper,
                                        final MagMpiProps mpiProps) {
        this.schemeMapper = schemeMapper;
        this.mpiProps = mpiProps;
    }

    public Resource translateToFhir(final byte[] input,
                                    final Map<String, Object> parameters) {
        try {
            return this.doTranslate(input);
        } catch (final BaseServerResponseException controlledException) {
            throw controlledException;
        } catch (final JAXBException parsingException) {
            throw new InvalidRequestException("Failed parsing ITI-47 response", parsingException);
        } catch (final Exception otherException) {
            throw new InvalidRequestException("Unexpected exception during ITI-47 response processing", otherException);
        }
    }

    public Resource doTranslate(final byte[] input) throws Exception {
        final PRPAIN201306UV02Type pdqResponse = HL7V3Transformer.unmarshallMessage(PRPAIN201306UV02Type.class,
                                                                                    new ByteArrayInputStream(input));
        final PRPAIN201306UV02MFMIMT700711UV01ControlActProcess controlAct = pdqResponse.getControlActProcess();

        // OK NF AE
        verifyAck(controlAct.getQueryAck().getQueryResponseCode(), pdqResponse.getAcknowledgement(), "ITI-47");

        final List<PRPAIN201306UV02MFMIMT700711UV01Subject1> subjects = controlAct.getSubject();
        if (this.mpiProps.isChPdqmConstraints() && subjects.size() > 5) {
            // https://github.com/i4mi/MobileAccessGateway/issues/171
            final var operationOutcome = new OperationOutcome();
            var code = new CodeableConcept();
            code.addCoding()
                    .setSystem("urn:oid:1.3.6.1.4.1.19376.1.2.27.1")
                    .setCode("LivingSubjectAdministrativeGenderRequested")
                    .setDisplay("LivingSubjectAdministrativeGenderRequested");
            operationOutcome.addIssue()
                    .setSeverity(OperationOutcome.IssueSeverity.WARNING)
                    .setCode(OperationOutcome.IssueType.INCOMPLETE)
                    .setDetails(code);

            code = new CodeableConcept();
            code.addCoding()
                    .setSystem("urn:oid:1.3.6.1.4.1.19376.1.2.27.1")
                    .setCode("LivingSubjectBirthPlaceNameRequested")
                    .setDisplay("LivingSubjectBirthPlaceNameRequested");
            operationOutcome.addIssue()
                    .setSeverity(OperationOutcome.IssueSeverity.WARNING)
                    .setCode(OperationOutcome.IssueType.INCOMPLETE)
                    .setDetails(code);

            code = new CodeableConcept();
            code.addCoding()
                    .setSystem("urn:oid:2.16.756.5.30.1.127.3.10.17")
                    .setCode("BirthNameRequested")
                    .setDisplay("BirthNameRequested");
            operationOutcome.addIssue()
                    .setSeverity(OperationOutcome.IssueSeverity.WARNING)
                    .setCode(OperationOutcome.IssueType.INCOMPLETE)
                    .setDetails(code);

            return operationOutcome;
        }

        final var bundle = new Bundle();

        // Iterate over all subjects (patients) and convert them
        for (PRPAIN201306UV02MFMIMT700711UV01Subject1 subject : subjects) {
            final PRPAMT201310UV02Patient patient = subject.getRegistrationEvent().getSubject1().getPatient();
            final PRPAMT201310UV02Person patientPerson = patient.getPatientPerson();

            if (patient.getId().isEmpty()) {
                continue;
            }

            final Patient result = new Patient();

            for (final II patientId : patient.getId()) {
                if (patientId.getRoot() == null && patientId.getExtension() == null) continue;

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
            }

            // Generate an ID if it's missing
            if (!result.hasId()) {
                result.setId(UUID.randomUUID().toString());
            }

            for (PRPAMT201310UV02OtherIDs otherIds : patient.getPatientPerson().getAsOtherIDs()) {
                for (II patientId : otherIds.getId()) {
                    if (patientId.getRoot() == null && patientId.getExtension() == null) continue;

                    if (this.mpiProps.isChPdqmConstraints()) {
                        if (this.mpiProps.getOids().getMpiPid().equals(patientId.getRoot()) || EPR_SPID_OID.equals(
                                patientId.getRoot())) {
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
                }
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

            final float score = Optional.ofNullable(patient.getSubjectOf1())
                            .map(JavaUtils::firstOrNull)
                            .map(PRPAMT201310UV02Subject::getQueryMatchObservation)
                            .map(PRPAMT201310UV02QueryMatchObservation::getValue)
                            .map(Hl7v3Mappers::toText)
                            .map(Integer::parseInt)
                            .map(i -> i / 100f)
                            .orElse(1.0f);
            final String scoreCode;
            if (score >= 0.75) {
                scoreCode = "certain";
            } else if (score >= 0.5) {
                scoreCode = "probable";
            } else {
                scoreCode = "possible";
            }

            final var search = new Bundle.BundleEntrySearchComponent()
                    .setMode(Bundle.SearchEntryMode.MATCH)
                    .setScore(score);
            search.addExtension("http://hl7.org/fhir/StructureDefinition/match-grade", new CodeType(scoreCode));

            bundle.addEntry()
                    .setFullUrl("urn:uuid:" + UUID.randomUUID())
                    .setResource(result)
                    .setSearch(search);
        }

        return bundle;
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
