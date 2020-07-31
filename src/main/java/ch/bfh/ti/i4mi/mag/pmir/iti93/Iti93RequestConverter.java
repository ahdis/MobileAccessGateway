package ch.bfh.ti.i4mi.mag.pmir.iti93;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.camel.Body;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.PatientCommunicationComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.codesystems.ContactPointUse;
import org.hl7.fhir.r4.model.codesystems.LinkType;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.BaseRequestConverter;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.pmir.PMIRRequestConverter;
import net.ihe.gazelle.hl7v3.coctmt090003UV01.COCTMT090003UV01AssignedEntity;
import net.ihe.gazelle.hl7v3.coctmt090003UV01.COCTMT090003UV01Organization;
import net.ihe.gazelle.hl7v3.coctmt150003UV03.COCTMT150003UV03ContactParty;
import net.ihe.gazelle.hl7v3.coctmt150003UV03.COCTMT150003UV03Organization;
import net.ihe.gazelle.hl7v3.datatypes.AD;
import net.ihe.gazelle.hl7v3.datatypes.AdxpCity;
import net.ihe.gazelle.hl7v3.datatypes.AdxpCountry;
import net.ihe.gazelle.hl7v3.datatypes.AdxpPostalCode;
import net.ihe.gazelle.hl7v3.datatypes.AdxpState;
import net.ihe.gazelle.hl7v3.datatypes.AdxpStreetAddressLine;
import net.ihe.gazelle.hl7v3.datatypes.CD;
import net.ihe.gazelle.hl7v3.datatypes.CE;
import net.ihe.gazelle.hl7v3.datatypes.CS;
import net.ihe.gazelle.hl7v3.datatypes.EnFamily;
import net.ihe.gazelle.hl7v3.datatypes.EnGiven;
import net.ihe.gazelle.hl7v3.datatypes.EnPrefix;
import net.ihe.gazelle.hl7v3.datatypes.EnSuffix;
import net.ihe.gazelle.hl7v3.datatypes.II;
import net.ihe.gazelle.hl7v3.datatypes.IVLTS;
import net.ihe.gazelle.hl7v3.datatypes.IVXBTS;
import net.ihe.gazelle.hl7v3.datatypes.ON;
import net.ihe.gazelle.hl7v3.datatypes.PN;
import net.ihe.gazelle.hl7v3.datatypes.TEL;
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
import net.ihe.gazelle.hl7v3.prpain201302UV02.PRPAIN201302UV02MFMIMT700701UV01ControlActProcess;
import net.ihe.gazelle.hl7v3.prpain201302UV02.PRPAIN201302UV02MFMIMT700701UV01RegistrationEvent;
import net.ihe.gazelle.hl7v3.prpain201302UV02.PRPAIN201302UV02MFMIMT700701UV01Subject1;
import net.ihe.gazelle.hl7v3.prpain201302UV02.PRPAIN201302UV02MFMIMT700701UV01Subject2;
import net.ihe.gazelle.hl7v3.prpain201302UV02.PRPAIN201302UV02Type;
import net.ihe.gazelle.hl7v3.prpain201309UV02.PRPAIN201309UV02QUQIMT021001UV01ControlActProcess;
import net.ihe.gazelle.hl7v3.prpain201309UV02.PRPAIN201309UV02Type;
import net.ihe.gazelle.hl7v3.prpamt201301UV02.PRPAMT201301UV02Patient;
import net.ihe.gazelle.hl7v3.prpamt201301UV02.PRPAMT201301UV02Person;
import net.ihe.gazelle.hl7v3.prpamt201302UV02.PRPAMT201302UV02Patient;
import net.ihe.gazelle.hl7v3.voc.ActClass;
import net.ihe.gazelle.hl7v3.voc.ActClassControlAct;
import net.ihe.gazelle.hl7v3.voc.ActMood;
import net.ihe.gazelle.hl7v3.voc.CommunicationFunctionType;
import net.ihe.gazelle.hl7v3.voc.EntityClass;
import net.ihe.gazelle.hl7v3.voc.EntityClassDevice;
import net.ihe.gazelle.hl7v3.voc.EntityClassOrganization;
import net.ihe.gazelle.hl7v3.voc.EntityDeterminer;
import net.ihe.gazelle.hl7v3.voc.ParticipationTargetSubject;
import net.ihe.gazelle.hl7v3.voc.ParticipationType;
import net.ihe.gazelle.hl7v3.voc.RoleClassAssignedEntity;
import net.ihe.gazelle.hl7v3.voc.RoleClassContact;
import net.ihe.gazelle.hl7v3.voc.XActMoodIntentEvent;
import net.ihe.gazelle.hl7v3transformer.HL7V3Transformer;

public class Iti93RequestConverter extends PMIRRequestConverter {

	@Autowired
	private Config config;
	
	public String iti93ToIti44Converter(@Body Bundle requestBundle) throws JAXBException {
		if (requestBundle.getType() != BundleType.MESSAGE) throw new InvalidRequestException("Bundle type must be message");
		
		BundleEntryComponent headerComponent = requestBundle.getEntryFirstRep();
		if (headerComponent == null) throw new InvalidRequestException("First bundle entry must be MessageHeader.");
		Resource headerResource = headerComponent.getResource();
		if (headerResource==null || !(headerResource instanceof MessageHeader)) throw new InvalidRequestException("First bundle entry must be MessageHeader.");
		MessageHeader header = (MessageHeader) headerResource;
		
		if (!("urn:ihe:iti:pmir:2019:patient-feed".equals(header.getEventUriType().getValue()))) throw new InvalidRequestException("Wrong eventUri");
		
				
		PRPAIN201301UV02Type resultMsg = new PRPAIN201301UV02Type();		
		  resultMsg.setITSVersion("XML_1.0");
		  //String UUID.randomUUID().toString();
		  resultMsg.setId(new II(config.getPixQueryOid(), uniqueId()));
		  resultMsg.setCreationTime(new TS(Timestamp.now().toHL7())); // Now
		  resultMsg.setProcessingCode(new CS("T", null ,null));
		  resultMsg.setProcessingModeCode(new CS("T", null, null));
		  resultMsg.setInteractionId(new II("2.16.840.1.113883.1.18", "PRPA_IN201301UV02"));
		  resultMsg.setAcceptAckCode(new CS("AL", null, null));
		
		  MCCIMT000100UV01Receiver receiver = new MCCIMT000100UV01Receiver();
		  resultMsg.addReceiver(receiver);
		  receiver.setTypeCode(CommunicationFunctionType.RCV);
		  
		  MCCIMT000100UV01Device receiverDevice = new MCCIMT000100UV01Device();
		  receiver.setDevice(receiverDevice );
		  receiverDevice.setClassCode(EntityClassDevice.DEV);
		  receiverDevice.setDeterminerCode(EntityDeterminer.INSTANCE);
		  receiverDevice.setId(Collections.singletonList(new II(config.getPixReceiverOid(), null)));
		  
		  MCCIMT000100UV01Sender sender = new MCCIMT000100UV01Sender();
		  resultMsg.setSender(sender);
		  sender.setTypeCode(CommunicationFunctionType.SND);
		  
		  MCCIMT000100UV01Device senderDevice = new MCCIMT000100UV01Device();
		  sender.setDevice(senderDevice);
		  senderDevice.setClassCode(EntityClassDevice.DEV);
		  senderDevice.setDeterminerCode(EntityDeterminer.INSTANCE);
		  senderDevice.setId(Collections.singletonList(new II(config.getPixMySenderOid(), null)));
		 
		  PRPAIN201301UV02MFMIMT700701UV01ControlActProcess controlActProcess = new PRPAIN201301UV02MFMIMT700701UV01ControlActProcess();		  
		  resultMsg.setControlActProcess(controlActProcess);
		  controlActProcess.setClassCode(ActClassControlAct.CACT); // ???
		  controlActProcess.setMoodCode(XActMoodIntentEvent.EVN); // ???
		  controlActProcess.setCode(new CD("PRPA_TE201301UV02","2.16.840.1.113883.1.18", null)); // ???
				  
		
	    for (BundleEntryComponent entry : requestBundle.getEntry()) {	    	
	    	if (entry.getResource() instanceof Patient) {
	    		HTTPVerb method = entry.getRequest().getMethod();
		    	if (method == null) throw new InvalidRequestException("HTTP verb missing in Bundle for Patient resource.");
		    			    			    	
		    	Patient in = (Patient) entry.getResource();
		    			    			    	
		    	PRPAIN201301UV02MFMIMT700701UV01Subject1 subject = new PRPAIN201301UV02MFMIMT700701UV01Subject1();		    	
			  controlActProcess.addSubject(subject);
			  subject.setTypeCode("SUBJ");
			  subject.setContextConductionInd(false); // ???
			  
			  PRPAIN201301UV02MFMIMT700701UV01RegistrationEvent registrationEvent = new PRPAIN201301UV02MFMIMT700701UV01RegistrationEvent();			  
			  subject.setRegistrationEvent(registrationEvent);
			  registrationEvent.setClassCode(ActClass.REG);
			  registrationEvent.setMoodCode(ActMood.EVN);
			  registrationEvent.setStatusCode(new CS("active",null,null)); // ???
			  
			  PRPAIN201301UV02MFMIMT700701UV01Subject2 subject1 = new PRPAIN201301UV02MFMIMT700701UV01Subject2();
			  			  
			  registrationEvent.setSubject1(subject1);
			  subject1.setTypeCode(ParticipationTargetSubject.SBJ);
			  
			  PRPAMT201301UV02Patient patient = new PRPAMT201301UV02Patient();			  
			  subject1.setPatient(patient);
			  patient.setClassCode("PAT");
			  			  
			  patient.setStatusCode(new CS("active", null ,null)); //???
			  
			  PRPAMT201301UV02Person patientPerson = new PRPAMT201301UV02Person();
			  patient.setPatientPerson(patientPerson);
			  patientPerson.setClassCode(EntityClass.PSN);
			  patientPerson.setDeterminerCode(EntityDeterminer.INSTANCE);
		    	
			  // TODO How is the correct mapping done?
			    for (Identifier id : in.getIdentifier()) {
			    	patient.addId(new II(getScheme(id.getSystem()),id.getValue()));
			    }
		    	
		    	for (HumanName name : in.getName()) {
		    		PN nameElement = new PN();
		    		if (name.hasFamily()) nameElement.addFamily(element(EnFamily.class, name.getFamily()));
		    		for (StringType given : name.getGiven()) nameElement.addGiven(element(EnGiven.class, given.getValue()));
		    		for (StringType prefix : name.getPrefix()) nameElement.addPrefix(element(EnPrefix.class, prefix.getValue()));
		    		for (StringType suffix : name.getSuffix()) nameElement.addSuffix(element(EnSuffix.class, suffix.getValue()));
		    		if (name.hasPeriod()) nameElement.addValidTime(transform(name.getPeriod()));		    		
					patientPerson.addName(nameElement );	
		    	}
		    	
		    	
		    	patientPerson.setBirthTime(transform(in.getBirthDateElement()));
		    	if (in.hasGender()) {
			        switch(in.getGender()) {
			        case MALE:patientPerson.setAdministrativeGenderCode(new CE("M","Male","2.16.840.1.113883.12.1"));break;
			        case FEMALE:patientPerson.setAdministrativeGenderCode(new CE("F","Female","2.16.840.1.113883.12.1"));break;
			        case OTHER:patientPerson.setAdministrativeGenderCode(new CE("A","Ambiguous","2.16.840.1.113883.12.1"));break;
			        case UNKNOWN:patientPerson.setAdministrativeGenderCode(new CE("U","Unknown","2.16.840.1.113883.12.1"));break;
			        }
		    	}
		        
		        for (Address address : in.getAddress()) {
		        	AD addr = new AD();

		        	// TODO Missing: district, type, use
		        	if (address.hasCity()) addr.addCity(element(AdxpCity.class, address.getCity()));
		        	if (address.hasCountry()) addr.addCountry(element(AdxpCountry.class, address.getCountry()));
		        	if (address.hasPostalCode()) addr.addPostalCode(element(AdxpPostalCode.class, address.getPostalCode()));
		        	if (address.hasState()) addr.addState(element(AdxpState.class, address.getState()));
		        	if (address.hasLine()) for (StringType line : address.getLine()) addr.addStreetAddressLine(element(AdxpStreetAddressLine.class, line.getValue()));
		        	if (address.hasPeriod()) addr.addUseablePeriod(transform(address.getPeriod()));
					patientPerson.addAddr(addr);
		        }
		    	
		        for (ContactPoint contactPoint : in.getTelecom()) {                    
					patientPerson.addTelecom(transform(contactPoint));
		        }
		        
		        List<II> orgIds = new ArrayList<II>();
		        Organization managingOrg = getManagingOrganization(in);
		        for (Identifier id : managingOrg.getIdentifier()) {
		        	orgIds.add(new II(getScheme(id.getSystem()), null));
		        }
		        
		        
		        COCTMT150003UV03Organization providerOrganization = new COCTMT150003UV03Organization();
				patient.setProviderOrganization(providerOrganization);
				providerOrganization.setClassCode(EntityClassOrganization.ORG);
				providerOrganization.setDeterminerCode(EntityDeterminer.INSTANCE);
		        				
				providerOrganization.setId(orgIds);
				ON name = null;
				if (managingOrg.hasName()) {	
					name = new ON();
					name.setMixed(Collections.singletonList(managingOrg.getName()));
					providerOrganization.setName(Collections.singletonList(name));
				}
								
				COCTMT150003UV03ContactParty contactParty = new COCTMT150003UV03ContactParty();
				contactParty.setClassCode(RoleClassContact.CON);
                for (ContactPoint contactPoint : managingOrg.getTelecom()) {
                	contactParty.addTelecom(transform(contactPoint));
				}				
				providerOrganization.setContactParty(Collections.singletonList(contactParty));
				
				MFMIMT700701UV01Custodian custodian = new MFMIMT700701UV01Custodian();
				registrationEvent.setCustodian(custodian );
				custodian.setTypeCode(ParticipationType.CST);
				
				COCTMT090003UV01AssignedEntity assignedEntity = new COCTMT090003UV01AssignedEntity();
				custodian.setAssignedEntity(assignedEntity);
				assignedEntity.setClassCode(RoleClassAssignedEntity.ASSIGNED);
				assignedEntity.setId(orgIds);
				
				COCTMT090003UV01Organization assignedOrganization = new COCTMT090003UV01Organization();
				assignedEntity.setAssignedOrganization(assignedOrganization );
				assignedOrganization.setClassCode(EntityClassOrganization.ORG);
				assignedOrganization.setDeterminerCode(EntityDeterminer.INSTANCE);
				if (managingOrg.hasName()) {	
				  assignedOrganization.setName(Collections.singletonList(name));
				}
	    	}
	    	
	    	
	    }
	    
	    ByteArrayOutputStream out = new ByteArrayOutputStream();	    
	    HL7V3Transformer.marshallMessage(PRPAIN201301UV02Type.class, out, resultMsg);
	    System.out.println("POST CONVERT");
	    String outArray = new String(out.toByteArray()); 
	    System.out.println(outArray);
	    return outArray;
	}
	
	private Organization getManagingOrganization(Patient in) {
		Reference org = in.getManagingOrganization();
		if (org == null) return null;		
        String targetRef = org.getReference();		
		List<Resource> resources = in.getContained();		
		for (Resource resource : resources) {			
			if (targetRef.equals(resource.getId()) && resource instanceof Organization) {
                return (Organization) resource;
			}
		}
		return null;
	}
}
