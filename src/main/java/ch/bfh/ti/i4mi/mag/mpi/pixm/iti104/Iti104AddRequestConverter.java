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

package ch.bfh.ti.i4mi.mag.mpi.pixm.iti104;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.config.props.MagMpiProps;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import ch.bfh.ti.i4mi.mag.mhd.SchemeMapper;
import ch.bfh.ti.i4mi.mag.mhd.iti65.Iti65RequestConverter;
import ch.bfh.ti.i4mi.mag.mpi.pmir.PMIRRequestConverter;
import jakarta.xml.bind.JAXBException;
import net.ihe.gazelle.hl7v3.coctmt090003UV01.COCTMT090003UV01AssignedEntity;
import net.ihe.gazelle.hl7v3.coctmt090003UV01.COCTMT090003UV01Organization;
import net.ihe.gazelle.hl7v3.coctmt150003UV03.COCTMT150003UV03ContactParty;
import net.ihe.gazelle.hl7v3.coctmt150003UV03.COCTMT150003UV03Organization;
import net.ihe.gazelle.hl7v3.datatypes.AD;
import net.ihe.gazelle.hl7v3.datatypes.BL;
import net.ihe.gazelle.hl7v3.datatypes.CD;
import net.ihe.gazelle.hl7v3.datatypes.CE;
import net.ihe.gazelle.hl7v3.datatypes.CS;
import net.ihe.gazelle.hl7v3.datatypes.II;
import net.ihe.gazelle.hl7v3.datatypes.INT;
import net.ihe.gazelle.hl7v3.datatypes.ON;
import net.ihe.gazelle.hl7v3.datatypes.TS;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Device;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Receiver;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Sender;
import net.ihe.gazelle.hl7v3.mfmimt700701UV01.MFMIMT700701UV01Custodian;
import net.ihe.gazelle.hl7v3.prpain201301UV02.PRPAIN201301UV02MFMIMT700701UV01ControlActProcess;
import net.ihe.gazelle.hl7v3.prpain201301UV02.PRPAIN201301UV02MFMIMT700701UV01RegistrationEvent;
import net.ihe.gazelle.hl7v3.prpain201301UV02.PRPAIN201301UV02MFMIMT700701UV01Subject1;
import net.ihe.gazelle.hl7v3.prpain201301UV02.PRPAIN201301UV02MFMIMT700701UV01Subject2;
import net.ihe.gazelle.hl7v3.prpain201301UV02.PRPAIN201301UV02Type;
import net.ihe.gazelle.hl7v3.prpamt201301UV02.PRPAMT201301UV02LanguageCommunication;
import net.ihe.gazelle.hl7v3.prpamt201301UV02.PRPAMT201301UV02Patient;
import net.ihe.gazelle.hl7v3.prpamt201301UV02.PRPAMT201301UV02Person;
import net.ihe.gazelle.hl7v3.prpamt201302UV02.PRPAMT201302UV02PatientId;
import net.ihe.gazelle.hl7v3.voc.ActClass;
import net.ihe.gazelle.hl7v3.voc.ActClassControlAct;
import net.ihe.gazelle.hl7v3.voc.ActMood;
import net.ihe.gazelle.hl7v3.voc.CommunicationFunctionType;
import net.ihe.gazelle.hl7v3.voc.EntityClass;
import net.ihe.gazelle.hl7v3.voc.EntityClassDevice;
import net.ihe.gazelle.hl7v3.voc.EntityClassOrganization;
import net.ihe.gazelle.hl7v3.voc.EntityDeterminer;
import net.ihe.gazelle.hl7v3.voc.NullFlavor;
import net.ihe.gazelle.hl7v3.voc.ParticipationTargetSubject;
import net.ihe.gazelle.hl7v3.voc.ParticipationType;
import net.ihe.gazelle.hl7v3.voc.RoleClassAssignedEntity;
import net.ihe.gazelle.hl7v3.voc.RoleClassContact;
import net.ihe.gazelle.hl7v3.voc.XActMoodIntentEvent;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.PatientCommunicationComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;
import org.openehealth.ipf.platform.camel.ihe.hl7v3.core.converters.JaxbHl7v3Converters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static ch.bfh.ti.i4mi.mag.mhd.iti65.Iti65RequestConverter.noPrefix;

/**
 * ITI-104 Patient Identity Feed (add a new patient)
 *
 * @author alexander kreutz
 *
 */
public class Iti104AddRequestConverter extends PMIRRequestConverter {

    protected final MagMpiProps.MagMpiOidsProps mpiOidsProps;
    protected final String organizationName;

    public Iti104AddRequestConverter(final SchemeMapper schemeMapper,
                                     final MagProps magProps) {
        super(schemeMapper);
        this.mpiOidsProps = magProps.getMpi().getOids();
        this.organizationName = magProps.getOrganizationName();
    }

    /**
     * add a new patient
     *
     * @return
     * @throws JAXBException
     */
    public String doCreate(Patient in, Identifier identifier) throws JAXBException {

        PRPAIN201301UV02Type resultMsg = new PRPAIN201301UV02Type();
        resultMsg.setITSVersion("XML_1.0");
        // String UUID.randomUUID().toString();
        resultMsg.setId(new II(this.mpiOidsProps.getSender(), uniqueId()));
        resultMsg.setCreationTime(new TS(Timestamp.now().toHL7())); // Now
        resultMsg.setProcessingCode(new CS("T", null, null));
        resultMsg.setProcessingModeCode(new CS("T", null, null));
        resultMsg.setInteractionId(new II("2.16.840.1.113883.1.18", "PRPA_IN201301UV02"));
        resultMsg.setAcceptAckCode(new CS("AL", null, null));

        MCCIMT000100UV01Receiver receiver = new MCCIMT000100UV01Receiver();
        resultMsg.addReceiver(receiver);
        receiver.setTypeCode(CommunicationFunctionType.RCV);

        MCCIMT000100UV01Device receiverDevice = new MCCIMT000100UV01Device();
        receiver.setDevice(receiverDevice);
        receiverDevice.setClassCode(EntityClassDevice.DEV);
        receiverDevice.setDeterminerCode(EntityDeterminer.INSTANCE);
        receiverDevice.setId(Collections.singletonList(new II(this.mpiOidsProps.getReceiver(), null)));

        MCCIMT000100UV01Sender sender = new MCCIMT000100UV01Sender();
        resultMsg.setSender(sender);
        sender.setTypeCode(CommunicationFunctionType.SND);

        MCCIMT000100UV01Device senderDevice = new MCCIMT000100UV01Device();
        sender.setDevice(senderDevice);
        senderDevice.setClassCode(EntityClassDevice.DEV);
        senderDevice.setDeterminerCode(EntityDeterminer.INSTANCE);
        senderDevice.setId(Collections.singletonList(new II(this.mpiOidsProps.getSender(), null)));

        PRPAIN201301UV02MFMIMT700701UV01ControlActProcess controlActProcess = new PRPAIN201301UV02MFMIMT700701UV01ControlActProcess();
        resultMsg.setControlActProcess(controlActProcess);
        controlActProcess.setClassCode(ActClassControlAct.CACT);
        controlActProcess.setMoodCode(XActMoodIntentEvent.EVN);
        controlActProcess.setCode(new CD("PRPA_TE201301UV02", null, "2.16.840.1.113883.1.18"));

        PRPAIN201301UV02MFMIMT700701UV01Subject1 subject = new PRPAIN201301UV02MFMIMT700701UV01Subject1();
        controlActProcess.addSubject(subject);
        subject.setTypeCode("SUBJ");
        subject.setContextConductionInd(false); // ???

        PRPAIN201301UV02MFMIMT700701UV01RegistrationEvent registrationEvent = new PRPAIN201301UV02MFMIMT700701UV01RegistrationEvent();
        subject.setRegistrationEvent(registrationEvent);
        registrationEvent.setClassCode(ActClass.REG);
        registrationEvent.setMoodCode(ActMood.EVN);
        registrationEvent.setStatusCode(new CS("active", null, null)); // ???

        PRPAIN201301UV02MFMIMT700701UV01Subject2 subject1 = new PRPAIN201301UV02MFMIMT700701UV01Subject2();

        registrationEvent.setSubject1(subject1);
        subject1.setTypeCode(ParticipationTargetSubject.SBJ);

        PRPAMT201301UV02Patient patient = new PRPAMT201301UV02Patient();
        subject1.setPatient(patient);
        patient.setClassCode("PAT");

        patient.setStatusCode(new CS("active", null, null)); // ???

        PRPAMT201301UV02Person patientPerson = new PRPAMT201301UV02Person();
        patient.setPatientPerson(patientPerson);
        patientPerson.setClassCode(EntityClass.PSN);
        patientPerson.setDeterminerCode(EntityDeterminer.INSTANCE);

        patient.addId(patientIdentifier(identifier));
        boolean inHeaderAndRequest = false;
        for (Identifier id : in.getIdentifier()) {
            if (id.getSystem() != null && id.getSystem().equals(identifier.getSystem()) && id.getValue() != null && id.getValue().equals(
                    identifier.getValue())) {
                inHeaderAndRequest = true;
            } else {
                patient.addId(patientIdentifier(id));
            }
        }
        if (!inHeaderAndRequest) {
            throw new InvalidRequestException("Patient identifier in header and request do not match");
        }

        for (HumanName name : in.getName()) {
            patientPerson.addName(transform(name));
        }

        patientPerson.setBirthTime(transform(in.getBirthDateElement()));
        if (in.hasGender()) {
            switch (in.getGender()) {
                case MALE:
                    patientPerson.setAdministrativeGenderCode(new CE("M", "Male", "2.16.840.1.113883.12.1"));
                    break;
                case FEMALE:
                    patientPerson.setAdministrativeGenderCode(new CE("F", "Female", "2.16.840.1.113883.12.1"));
                    break;
                case OTHER:
                    patientPerson.setAdministrativeGenderCode(new CE("A", "Ambiguous", "2.16.840.1.113883.12.1"));
                    break;
                case UNKNOWN:
                    patientPerson.setAdministrativeGenderCode(new CE("U", "Unknown", "2.16.840.1.113883.12.1"));
                    break;
            }
        }

        if (in.hasAddress())
            patientPerson.setAddr(new ArrayList<AD>());
        for (Address address : in.getAddress()) {
            patientPerson.addAddr(transform(address));
        }

        for (ContactPoint contactPoint : in.getTelecom()) {
            patientPerson.addTelecom(transform(contactPoint));
        }

        if (in.hasDeceasedBooleanType()) {
            patientPerson.setDeceasedInd(new BL(in.getDeceasedBooleanType().getValue()));
        }
        if (in.hasDeceasedDateTimeType()) {
            patientPerson.setDeceasedTime(transform(in.getDeceasedDateTimeType()));
        }
        if (in.hasMultipleBirthBooleanType()) {
            patientPerson.setMultipleBirthInd(new BL(in.getMultipleBirthBooleanType().getValue()));
        }
        if (in.hasMultipleBirthIntegerType()) {
            patientPerson.setMultipleBirthOrderNumber(new INT(in.getMultipleBirthIntegerType().getValue()));
        }
        if (in.hasMaritalStatus()) {
            patientPerson.setMaritalStatusCode(transform(in.getMaritalStatus()));
        }
        if (in.hasCommunication()) {
            for (PatientCommunicationComponent pcc : in.getCommunication()) {
                PRPAMT201301UV02LanguageCommunication languageCommunication = new PRPAMT201301UV02LanguageCommunication();
                languageCommunication.setLanguageCode(transform(pcc.getLanguage()));
                // NULL POINTER EXCEPTION
                if (pcc.hasPreferred())
                    languageCommunication.setPreferenceInd(new BL(pcc.getPreferred()));
                patientPerson.addLanguageCommunication(languageCommunication);
            }
        }

        patient.setProviderOrganization(this.generateProviderOrganization(in));
        registrationEvent.setCustodian(this.generateCustodian());

        return JaxbHl7v3Converters.PRPAIN201301UV02toXml(resultMsg);
    }

    Patient findPatient(Reference ref, Map<String, BundleEntryComponent> entriesbyReference, Patient current) {
        BundleEntryComponent entry = entriesbyReference.get(ref.getReference());
        if (entry != null)
            return (Patient) entry.getResource();
        for (Resource res : current.getContained()) {
            if (ref.getReference().equals(res.getId()))
                return (Patient) res;
        }
        return null;
    }

    public II patientIdentifier(Identifier id) {
        String assigner = null;
        if (id.hasAssigner())
            assigner = id.getAssigner().getDisplay();
        return new II(noPrefix(id.getSystem()), id.getValue(), assigner);
    }

    public PRPAMT201302UV02PatientId patientIdentifierUpd(Identifier id) {
        return new PRPAMT201302UV02PatientId(noPrefix(id.getSystem()), id.getValue());
    }

    COCTMT150003UV03Organization generateProviderOrganization(final Patient in) {
        // Provider organization
        // IDs are taken from the systems of Patient.id
        // The name is taken from configuration
        final var providerOrganization = new COCTMT150003UV03Organization();
        providerOrganization.setClassCode(EntityClassOrganization.ORG);
        providerOrganization.setDeterminerCode(EntityDeterminer.INSTANCE);

        providerOrganization.setId(in.getIdentifier().stream()
                                           .map(Identifier::getSystem)
                                           .map(Iti65RequestConverter::noPrefix)
                                           .map(system -> new II(system, null))
                                           .toList());
        final var name = new ON();
        name.setMixed(List.of(this.organizationName));
        providerOrganization.setName(List.of(name));

        final var contactParty = new COCTMT150003UV03ContactParty();
        contactParty.setNullFlavor(NullFlavor.UNK);
        contactParty.setClassCode(RoleClassContact.CON);
        providerOrganization.setContactParty(List.of(contactParty));
        return providerOrganization;
    }

    MFMIMT700701UV01Custodian generateCustodian() {
        final var custodian = new MFMIMT700701UV01Custodian();
        custodian.setTypeCode(ParticipationType.CST);

        final var assignedEntity = new COCTMT090003UV01AssignedEntity();
        custodian.setAssignedEntity(assignedEntity);
        assignedEntity.setClassCode(RoleClassAssignedEntity.ASSIGNED);
        assignedEntity.setId(List.of(new II(noPrefix(this.mpiOidsProps.getCustodian()), null)));

        final var assignedOrganization = new COCTMT090003UV01Organization();
        assignedEntity.setAssignedOrganization(assignedOrganization);
        assignedOrganization.setClassCode(EntityClassOrganization.ORG);
        assignedOrganization.setDeterminerCode(EntityDeterminer.INSTANCE);

        final var name = new ON();
        name.setMixed(List.of(this.organizationName));
        assignedOrganization.setName(List.of(name));

        return custodian;
    }
}
