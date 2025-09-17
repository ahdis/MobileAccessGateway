/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.bfh.ti.i4mi.mag.mpi.pixm.iti83;

import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.BaseRequestConverter;
import ch.bfh.ti.i4mi.mag.config.props.MagMpiProps;
import ch.bfh.ti.i4mi.mag.mhd.SchemeMapper;
import jakarta.xml.bind.JAXBException;
import net.ihe.gazelle.hl7v3.coctmt090100UV01.COCTMT090100UV01AssignedPerson;
import net.ihe.gazelle.hl7v3.datatypes.CD;
import net.ihe.gazelle.hl7v3.datatypes.CS;
import net.ihe.gazelle.hl7v3.datatypes.II;
import net.ihe.gazelle.hl7v3.datatypes.TS;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Device;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Receiver;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Sender;
import net.ihe.gazelle.hl7v3.prpain201309UV02.PRPAIN201309UV02QUQIMT021001UV01ControlActProcess;
import net.ihe.gazelle.hl7v3.prpain201309UV02.PRPAIN201309UV02Type;
import net.ihe.gazelle.hl7v3.prpamt201307UV02.PRPAMT201307UV02DataSource;
import net.ihe.gazelle.hl7v3.prpamt201307UV02.PRPAMT201307UV02ParameterList;
import net.ihe.gazelle.hl7v3.prpamt201307UV02.PRPAMT201307UV02PatientIdentifier;
import net.ihe.gazelle.hl7v3.prpamt201307UV02.PRPAMT201307UV02QueryByParameter;
import net.ihe.gazelle.hl7v3.quqimt021001UV01.QUQIMT021001UV01AuthorOrPerformer;
import net.ihe.gazelle.hl7v3.voc.ActClassControlAct;
import net.ihe.gazelle.hl7v3.voc.CommunicationFunctionType;
import net.ihe.gazelle.hl7v3.voc.EntityClassDevice;
import net.ihe.gazelle.hl7v3.voc.EntityDeterminer;
import net.ihe.gazelle.hl7v3.voc.RoleClassAssignedEntity;
import net.ihe.gazelle.hl7v3.voc.XActMoodIntentEvent;
import net.ihe.gazelle.hl7v3.voc.XParticipationAuthorPerformer;
import net.ihe.gazelle.hl7v3transformer.HL7V3Transformer;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.UriType;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;
import org.openehealth.ipf.platform.camel.ihe.hl7v3.core.converters.JaxbHl7v3Converters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

import static ch.bfh.ti.i4mi.mag.MagConstants.EPR_SPID_OID;
import static ch.bfh.ti.i4mi.mag.mhd.iti65.Iti65RequestConverter.noPrefix;

/**
 * ITI-83 to ITI-45 request converter
 *
 * @author alexander kreutz
 *
 */
public class Iti83RequestConverter extends BaseRequestConverter {
    private static final Logger log = LoggerFactory.getLogger(Iti83RequestConverter.class);

    private final MagMpiProps mpiProps;
    private final MagMpiProps.MagMpiOidsProps oids;

    public Iti83RequestConverter(final SchemeMapper schemeMapper,
                                 final MagMpiProps mpiProps) {
        super(schemeMapper);
        this.mpiProps = mpiProps;
        this.oids = mpiProps.getOids();
    }


    public OperationOutcome getTargetDomainNotRecognized() {
        final var outcome = new OperationOutcome();
        final OperationOutcomeIssueComponent issue = outcome.addIssue();
        issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
        issue.setCode(IssueType.CODEINVALID);
        issue.setDiagnostics("targetSystem not found");
        return outcome;
    }

    public OperationOutcome getSourceIdentifierMissing() {
        final var outcome = new OperationOutcome();
        final OperationOutcomeIssueComponent issue = outcome.addIssue();
        issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
        issue.setCode(IssueType.CODEINVALID);
        issue.setDiagnostics("sourceIdentifier is missing");
        return outcome;
    }

    public String iti83ToIti45Converter(Parameters parameters) throws JAXBException {
        List<Parameters.ParametersParameterComponent> targetSystemList = parameters.getParameters("targetSystem");
        Identifier sourceIdentifier = (Identifier) parameters.getParameter("sourceIdentifier").getValue();

        if (this.mpiProps.isChPixmConstraints()) {
            // https://fhir.ch/ig/ch-epr-fhir/iti-83.html#message-semantics-1
            if (sourceIdentifier == null) {
                log.error("sourceIdentifier is missing");
                throw new InvalidRequestException("sourceIdentifier is missing", getSourceIdentifierMissing());
            }

            if (sourceIdentifier.getSystem() == null || sourceIdentifier.getValue() == null) {
                log.error("sourceIdentifier system or value is missing");
                throw new InvalidRequestException("sourceIdentifier is missing", getSourceIdentifierMissing());
            }

            // FIXME https://gazelle.ihe.net/jira/servicedesk/customer/portal/8/EHS-820
            if (targetSystemList == null || (targetSystemList.size() == 0)) {
//			if (targetSystemList == null || (targetSystemList.size() != 2)) {
                log.error("targetSystem need to be 2..2");
                throw new ForbiddenOperationException("targetSystem need to be 2..2", getTargetDomainNotRecognized());
            }
            // FIXME https://gazelle.ihe.net/jira/servicedesk/customer/portal/8/EHS-820
            UriType uri1 = (UriType) targetSystemList.get(0).getValue();
            if (uri1.getValue().equals(EPR_SPID_OID)) {
                uri1.setValue("urn:oid:" + EPR_SPID_OID);
            }
            UriType uri2 = null;
            if (targetSystemList.size() > 1) {
                uri2 = (UriType) targetSystemList.get(1).getValue();
                if (uri2.getValue().equals(EPR_SPID_OID)) {
                    uri2.setValue("urn:oid:" + EPR_SPID_OID);
                }
            }
            if (!((uri1.equals("urn:oid:" + EPR_SPID_OID) && (uri2 == null || uri2.equals("urn:oid:" + this.oids.getMpiPid())) || (uri1.equals(
                    "urn:oid:" + this.oids.getMpiPid()) && (uri2 == null || uri2.equals("urn:oid:" + EPR_SPID_OID)))))) {
                log.error("targetSystem is not restricted to the Assigning authority of the community and the EPR-SPID");
                throw new ForbiddenOperationException(
                        "targetSystem is not restricted to the Assigning authority of the community and the EPR-SPID,",
                        getTargetDomainNotRecognized());
            }
        }

        PRPAIN201309UV02Type resultMsg = new PRPAIN201309UV02Type();
        resultMsg.setITSVersion("XML_1.0");
        // String UUID.randomUUID().toString();
        resultMsg.setId(new II(this.oids.getSender(), uniqueId()));
        resultMsg.setCreationTime(new TS(Timestamp.now().toHL7())); // Now
        resultMsg.setProcessingCode(new CS("T", null, null));
        resultMsg.setProcessingModeCode(new CS("T", null, null));
        resultMsg.setInteractionId(new II("2.16.840.1.113883.1.18", "PRPA_IN201309UV02"));
        resultMsg.setAcceptAckCode(new CS("AL", null, null));

        MCCIMT000100UV01Receiver receiver = new MCCIMT000100UV01Receiver();
        resultMsg.addReceiver(receiver);
        receiver.setTypeCode(CommunicationFunctionType.RCV);

        MCCIMT000100UV01Device receiverDevice = new MCCIMT000100UV01Device();
        receiver.setDevice(receiverDevice);
        receiverDevice.setClassCode(EntityClassDevice.DEV);
        receiverDevice.setDeterminerCode(EntityDeterminer.INSTANCE);
        receiverDevice.setId(Collections.singletonList(new II(this.oids.getReceiver(), null)));

        MCCIMT000100UV01Sender sender = new MCCIMT000100UV01Sender();
        resultMsg.setSender(sender);
        sender.setTypeCode(CommunicationFunctionType.SND);

        MCCIMT000100UV01Device senderDevice = new MCCIMT000100UV01Device();
        sender.setDevice(senderDevice);
        senderDevice.setClassCode(EntityClassDevice.DEV);
        senderDevice.setDeterminerCode(EntityDeterminer.INSTANCE);
        senderDevice.setId(Collections.singletonList(new II(this.oids.getSender(), null)));

        PRPAIN201309UV02QUQIMT021001UV01ControlActProcess controlActProcess = new PRPAIN201309UV02QUQIMT021001UV01ControlActProcess();
        resultMsg.setControlActProcess(controlActProcess);
        controlActProcess.setClassCode(ActClassControlAct.CACT);
        controlActProcess.setMoodCode(XActMoodIntentEvent.EVN);
        controlActProcess.setCode(new CD("PRPA_TE201309UV02", "2.16.840.1.113883.1.18", null));

        QUQIMT021001UV01AuthorOrPerformer authorOrPerformer = new QUQIMT021001UV01AuthorOrPerformer();
        authorOrPerformer.setTypeCode(XParticipationAuthorPerformer.AUT);

        COCTMT090100UV01AssignedPerson assignedPerson = new COCTMT090100UV01AssignedPerson();
        assignedPerson.setClassCode(RoleClassAssignedEntity.ASSIGNED);
        String assignedPersonId = this.mpiProps.getLocalPatientIdAssigningAuthority();
        if (assignedPersonId == null || assignedPersonId.length() == 0) assignedPersonId = this.oids.getCustodian();
        if (assignedPersonId == null || assignedPersonId.length() == 0) assignedPersonId = this.oids.getSender();
        assignedPerson.setId(Collections.singletonList(new II(assignedPersonId, null)));
        authorOrPerformer.setAssignedPerson(assignedPerson);
        controlActProcess.setAuthorOrPerformer(Collections.singletonList(authorOrPerformer));
        PRPAMT201307UV02QueryByParameter queryByParameter = new PRPAMT201307UV02QueryByParameter();
        controlActProcess.setQueryByParameter(queryByParameter);
        queryByParameter.setQueryId(new II(this.oids.getSender(), uniqueId()));
        queryByParameter.setStatusCode(new CS("new", null, null));
        queryByParameter.setResponsePriorityCode(new CS("I", null, null));

        PRPAMT201307UV02ParameterList parameterList = new PRPAMT201307UV02ParameterList();
        queryByParameter.setParameterList(parameterList);

        PRPAMT201307UV02PatientIdentifier patientIdentifier = new PRPAMT201307UV02PatientIdentifier();
        parameterList.addPatientIdentifier(patientIdentifier);
        String system = noPrefix(sourceIdentifier.getSystem());

        patientIdentifier.setValue(Collections.singletonList(new II(system, sourceIdentifier.getValue())));
        patientIdentifier.setSemanticsText(ST("Patient.id"));

        if (targetSystemList != null && !targetSystemList.isEmpty()) {
            for (Parameters.ParametersParameterComponent targetSystemType : targetSystemList) {
                UriType targetSystem = (UriType) targetSystemType.getValue();
                String sourceSystem = noPrefix(targetSystem.getValue());
                PRPAMT201307UV02DataSource dataSource = new PRPAMT201307UV02DataSource();
                parameterList.addDataSource(dataSource);
                dataSource.setValue(Collections.singletonList(new II(sourceSystem, null, null)));
                dataSource.setSemanticsText(ST("DataSource.id"));
            }
        }

        return JaxbHl7v3Converters.PRPAIN201309UV02toXml(resultMsg);
    }
}
