package ch.bfh.ti.i4mi.mag.pmir;

import net.ihe.gazelle.hl7v3.datatypes.*;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Device;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Receiver;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Sender;
import net.ihe.gazelle.hl7v3.prpain201305UV02.PRPAIN201305UV02QUQIMT021001UV01ControlActProcess;
import net.ihe.gazelle.hl7v3.prpain201305UV02.PRPAIN201305UV02Type;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02ParameterList;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02QueryByParameter;
import net.ihe.gazelle.hl7v3.voc.*;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;

import ch.bfh.ti.i4mi.mag.BaseRequestConverter;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;

import java.util.Collections;

/**
 * base class for PMIR/PIX request converters
 * @author alexander kreutz
 *
 */
public class PMIRRequestConverter extends BaseRequestConverter {

	public static <T extends net.ihe.gazelle.hl7v3.datatypes.ST> T element(Class<T> cl, String content) {
		try {
			T instance = cl.getDeclaredConstructor().newInstance();
			instance.addMixed(content);
			return instance;
		} catch (Exception e) { return null; }
	}
	
	public static IVLTS transform(Period period) {
		if (period == null) return null;
		IVLTS result = new IVLTS();
		if (period.hasStart()) result.setLow(transform(period.getStartElement()));
		if (period.hasEnd()) result.setHigh(transform(period.getEndElement()));
		return result;
	}
	
	public static IVXBTS transform(DateTimeType date) {
		IVXBTS result = new IVXBTS();
		result.setValue(date.getAsV3());
		return result;
	}
	
	public static IVXBTS transformTest(DateTimeType date) {
		IVXBTS result = new IVXBTS();
		String test = date.getAsV3();
		if (test.length()>8) test = test.substring(0, 8);
		result.setValue(test);
		return result;
	}
	
	public static TS transform(DateType date) {
		if (date == null || !(date.hasValue())) return null;
		TS result = new TS();
		result.setValue(date.getValueAsString().replace("-",""));
		return result;
	}
	
	public static String convertContactPointUse(ContactPoint.ContactPointUse use) {
		switch (use) {
		case HOME: return "HP"; 
		case WORK: return "WP"; 
		case TEMP: return "TMP";
		case OLD: return "BAD";
		case MOBILE: return "MC";
		default: return null;
		}
	}
	
	public static TEL transform(ContactPoint contactPoint) {
        TEL telecom = new TEL();
    			              
    	ContactPoint.ContactPointUse use = contactPoint.getUse();		        	
    	if (use != null) telecom.setUse(convertContactPointUse(use));
    	String telValue = contactPoint.getValue();
    	if (telValue != null && telValue.indexOf("@")>0 && !telValue.startsWith("mailto:")) telValue = "mailto:"+telValue;
    	telecom.setValue(telValue);
    	if (contactPoint.hasPeriod()) telecom.addUseablePeriod(transform(contactPoint.getPeriod()));
    	
    	return telecom;
	}
	
	
	public static AD transform(Address address) {
		AD addr = new AD();
	
		// TODO Missing: type, use
		if (address.hasCity()) addr.addCity(element(AdxpCity.class, address.getCity()));
		if (address.hasCountry()) addr.addCountry(element(AdxpCountry.class, address.getCountry()));
		if (address.hasDistrict()) addr.addCounty(element(AdxpCounty.class, address.getDistrict()));
		if (address.hasPostalCode()) addr.addPostalCode(element(AdxpPostalCode.class, address.getPostalCode()));
		if (address.hasState()) addr.addState(element(AdxpState.class, address.getState()));
		if (address.hasLine()) for (StringType line : address.getLine()) addr.addStreetAddressLine(element(AdxpStreetAddressLine.class, line.getValue()));
		if (address.hasPeriod()) addr.addUseablePeriod(transform(address.getPeriod()));
		if (address.hasUse()) {
			switch(address.getUse()) {		
				case HOME:addr.setUse("H");break;
				case WORK:addr.setUse("WP");break;
				case TEMP:addr.setUse("TMP");break;
				case OLD:addr.setUse("OLD");break;
		    }
		}
		

		return addr;
	}
	
	public static <T extends ENXP> T qualifier(StringType fhirNamePart, T namePart) {
		String qualifier = fhirNamePart.getExtensionString("http://hl7.org/fhir/StructureDefinition/iso21090-EN-qualifier");
        if (qualifier != null) namePart.setQualifier(qualifier);	
        return namePart;
	}
	
	public static PN transform(HumanName name) { 
		PN nameElement = new PN();
		if (name.hasFamily()) {
			EnFamily family = element(EnFamily.class, name.getFamily());
			if (name.hasUse() && name.getUse().equals(NameUse.MAIDEN)) family.setQualifier("BR");			
			nameElement.addFamily(qualifier(name.getFamilyElement(), family));
		}
		for (StringType given : name.getGiven()) nameElement.addGiven(qualifier(given, element(EnGiven.class, given.getValue())));
		for (StringType prefix : name.getPrefix()) nameElement.addPrefix(qualifier(prefix, element(EnPrefix.class, prefix.getValue())));
		for (StringType suffix : name.getSuffix()) nameElement.addSuffix(qualifier(suffix, element(EnSuffix.class, suffix.getValue())));
		if (name.hasPeriod()) nameElement.addValidTime(transform(name.getPeriod()));	
		
		return nameElement;
	}
	
	public static CE transform(CodeableConcept cc) {
		if (cc == null || !cc.hasCoding()) return null;
		Coding coding = cc.getCodingFirstRep();
		return new CE(coding.getCode(), coding.getDisplay(), coding.getSystem());
	}

    protected PRPAIN201305UV02Type initiateIti47Request(final String senderOid,
                                                        final String receiverOid) {
        final var request = new PRPAIN201305UV02Type();
        request.setITSVersion("XML_1.0");

        request.setId(new II(senderOid, uniqueId()));
        request.setCreationTime(new TS(Timestamp.now().toHL7())); // Now
        request.setProcessingCode(new CS("T", null ,null));
        request.setProcessingModeCode(new CS("T", null, null));
        request.setInteractionId(new II("2.16.840.1.113883.1.6", "PRPA_IN201305UV02"));
        request.setAcceptAckCode(new CS("AL", null, null));

        final var receiver = new MCCIMT000100UV01Receiver();
        request.addReceiver(receiver);
        receiver.setTypeCode(CommunicationFunctionType.RCV);

        final var receiverDevice = new MCCIMT000100UV01Device();
        receiver.setDevice(receiverDevice );
        receiverDevice.setClassCode(EntityClassDevice.DEV);
        receiverDevice.setDeterminerCode(EntityDeterminer.INSTANCE);
        receiverDevice.setId(Collections.singletonList(new II(receiverOid, null)));

        final var sender = new MCCIMT000100UV01Sender();
        request.setSender(sender);
        sender.setTypeCode(CommunicationFunctionType.SND);

        final var senderDevice = new MCCIMT000100UV01Device();
        sender.setDevice(senderDevice);
        senderDevice.setClassCode(EntityClassDevice.DEV);
        senderDevice.setDeterminerCode(EntityDeterminer.INSTANCE);
        senderDevice.setId(Collections.singletonList(new II(senderOid, null)));

        final var controlActProcess = new PRPAIN201305UV02QUQIMT021001UV01ControlActProcess();
        request.setControlActProcess(controlActProcess );
        controlActProcess.setClassCode(ActClassControlAct.CACT);
        controlActProcess.setMoodCode(XActMoodIntentEvent.EVN);
        controlActProcess.setCode(new CD("PRPA_TE201305UV02","2.16.840.1.113883.1.6", null));

        final var queryByParameter = new PRPAMT201306UV02QueryByParameter();
        controlActProcess.setQueryByParameter(queryByParameter );
        queryByParameter.setQueryId(new II(senderOid, uniqueId()));
        queryByParameter.setStatusCode(new CS("new", null, null));
        queryByParameter.setResponsePriorityCode(new CS("I", null, null));
        queryByParameter.setResponseModalityCode(new CS("R", null, null));

        final var parameterList = new PRPAMT201306UV02ParameterList();
        queryByParameter.setParameterList(parameterList);

        return request;
    }
}
