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

package ch.bfh.ti.i4mi.mag.mpi.pdqm.iti78;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateOrListParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.config.props.MagMpiProps;
import ch.bfh.ti.i4mi.mag.mhd.SchemeMapper;
import ch.bfh.ti.i4mi.mag.mpi.pmir.PMIRRequestConverter;
import jakarta.xml.bind.JAXBException;
import net.ihe.gazelle.hl7v3.datatypes.AD;
import net.ihe.gazelle.hl7v3.datatypes.AdxpCity;
import net.ihe.gazelle.hl7v3.datatypes.AdxpCountry;
import net.ihe.gazelle.hl7v3.datatypes.AdxpPostalCode;
import net.ihe.gazelle.hl7v3.datatypes.AdxpState;
import net.ihe.gazelle.hl7v3.datatypes.AdxpStreetAddressLine;
import net.ihe.gazelle.hl7v3.datatypes.CE;
import net.ihe.gazelle.hl7v3.datatypes.CS;
import net.ihe.gazelle.hl7v3.datatypes.EN;
import net.ihe.gazelle.hl7v3.datatypes.EnFamily;
import net.ihe.gazelle.hl7v3.datatypes.EnGiven;
import net.ihe.gazelle.hl7v3.datatypes.II;
import net.ihe.gazelle.hl7v3.datatypes.IVLTS;
import net.ihe.gazelle.hl7v3.datatypes.IVXBTS;
import net.ihe.gazelle.hl7v3.datatypes.PN;
import net.ihe.gazelle.hl7v3.datatypes.TEL;
import net.ihe.gazelle.hl7v3.prpain201305UV02.PRPAIN201305UV02Type;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02LivingSubjectAdministrativeGender;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02LivingSubjectBirthTime;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02LivingSubjectId;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02LivingSubjectName;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02MothersMaidenName;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02OtherIDsScopingOrganization;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02PatientAddress;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02PatientStatusCode;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02PatientTelecom;
import net.ihe.gazelle.hl7v3transformer.HL7V3Transformer;
import org.apache.camel.Header;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.openehealth.ipf.commons.ihe.fhir.iti78.Iti78SearchParameters;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Date;

import static ch.bfh.ti.i4mi.mag.MagConstants.EPR_SPID_OID;

/**
 * Convert ITI-78 to ITI-43 request
 *
 * @author alexander kreutz
 *
 */
public class Iti78RequestConverter extends PMIRRequestConverter {

    protected final MagMpiProps.MagMpiOidsProps mpiOidsProps;
    private final boolean isChEprspidAsPatientId;

    public Iti78RequestConverter(final SchemeMapper schemeMapper,
                                 final MagMpiProps magMpiProps) {
        super(schemeMapper);
        this.mpiOidsProps = magMpiProps.getOids();
        this.isChEprspidAsPatientId = magMpiProps.isChEprspidAsPatientId();
    }

    public IVXBTS transform(DateParam date) {
        DateTimeType dt = new DateTimeType(date.getValueAsString());
        return transform(dt);
    }

    public IVXBTS transform(Date date) {
        DateTimeType dt = new DateTimeType(date);
        return transform(dt);
    }

    /**
     * test version that cuts off the time information
     *
     * @param date
     * @return
     */
    public IVXBTS transformTest(Date date) {
        DateTimeType dt = new DateTimeType(date);
        return transformTest(dt);
    }

    public IVXBTS transformDay(Date date) {
        DateType dt = new DateType(date);
        IVXBTS result = new IVXBTS();
        result.setValue(dt.asStringValue().replace("-", ""));
        return result;
    }

    public String iti78ToIti47Converter(@Header("FhirRequestParameters") Iti78SearchParameters parameters)
            throws JAXBException {
        final PRPAIN201305UV02Type resultMsg = initiateIti47Request(this.mpiOidsProps.getSender(),
                                                                    this.mpiOidsProps.getReceiver());
        final var parameterList = resultMsg.getControlActProcess().getQueryByParameter().getParameterList();

        TokenParam id = parameters.get_id();
        if (id != null) {
            if (this.isChEprspidAsPatientId) {
                parameterList.addLivingSubjectId(createSubjectId(EPR_SPID_OID, id.getValue()));
            } else {
                String v = id.getValue();
                int idx = v.indexOf("-");
                if (idx > 0) {
                    parameterList.addLivingSubjectId(createSubjectId(v.substring(0, idx), v.substring(idx + 1)));
                }
            }
        }

        // active -> patientStatusCode
        TokenParam active = parameters.getActive();
        if (active != null) {
            String activeCode = "active";
            PRPAMT201306UV02PatientStatusCode patientStatusCode = new PRPAMT201306UV02PatientStatusCode();
            patientStatusCode.setValue(new CS(activeCode, null, null));
            parameterList.addPatientStatusCode(patientStatusCode);
        }

        // patientAddress
        StringParam postalCode = parameters.getPostalCode();
        StringParam state = parameters.getState();
        StringParam city = parameters.getCity();
        StringParam country = parameters.getCountry();
        StringParam address = parameters.getAddress();
        if (postalCode != null || state != null || city != null || country != null || address != null) {
            final var ad = new AD();
            if (postalCode != null) ad.addPostalCode(element(AdxpPostalCode.class, postalCode.getValue()));
            if (state != null) ad.addState(element(AdxpState.class, state.getValue()));
            if (city != null) ad.addCity(element(AdxpCity.class, city.getValue()));
            if (country != null) ad.addCountry(element(AdxpCountry.class, country.getValue()));
            if (address != null) ad.addStreetAddressLine(element(AdxpStreetAddressLine.class, address.getValue()));
            parameterList.addPatientAddress(createAddress(ad));
        }

        // livingSubjectBirthTime
        DateAndListParam birthdate = parameters.getBirthDate();
        if (birthdate != null) {
            IVLTS ivlts = new IVLTS();
            for (DateOrListParam birthdateOr : birthdate.getValuesAsQueryTokens()) {
                for (DateParam birthdateParam : birthdateOr.getValuesAsQueryTokens()) {

                    Date lDate = null;
                    Date hDate = null;
                    TemporalPrecisionEnum precision = birthdateParam.getPrecision();
                    ParamPrefixEnum prefix = birthdateParam.getPrefix();
                    if (prefix == null) prefix = ParamPrefixEnum.EQUAL;
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(birthdateParam.getValue());

                    switch (precision) {
                        case SECOND:
                            cal.set(Calendar.MILLISECOND, 0);
                            lDate = cal.getTime();
                            cal.set(Calendar.MILLISECOND, 1000);
                            hDate = cal.getTime();
                            break;
                        case MINUTE:
                            cal.set(Calendar.MILLISECOND, 0);
                            cal.set(Calendar.SECOND, 0);
                            lDate = cal.getTime();
                            cal.add(Calendar.MINUTE, 1);
                            hDate = cal.getTime();
                            break;
                        case DAY:
                            cal.set(Calendar.MILLISECOND, 0);
                            cal.set(Calendar.SECOND, 0);
                            cal.set(Calendar.MINUTE, 0);
                            cal.set(Calendar.HOUR_OF_DAY, 0);
                            lDate = cal.getTime();
                            cal.add(Calendar.DAY_OF_MONTH, 1);
                            hDate = cal.getTime();
                            break;
                        case MONTH:
                            int month = cal.get(Calendar.MONTH);
                            int year = cal.get(Calendar.YEAR);
                            cal.set(year, month, 1, 0, 0, 0);
                            lDate = cal.getTime();
                            cal.add(Calendar.MONTH, 1);
                            hDate = cal.getTime();
                            break;
                        case YEAR:
                            year = cal.get(Calendar.YEAR);
                            cal.set(year, 0, 1, 0, 0, 0);
                            lDate = cal.getTime();
                            cal.add(Calendar.YEAR, 1);
                            hDate = cal.getTime();
                            break;
                        case MILLI:
                        default:
                            lDate = cal.getTime();
                            cal.add(Calendar.MILLISECOND, 1);
                            hDate = cal.getTime();
                            break;
                    }

                    switch (prefix) {
                        case GREATERTHAN:
                            ivlts.setLow(transformTest(hDate));
                            break;
                        case LESSTHAN:
                            ivlts.setHigh(transformTest(lDate));
                            break;
                        case GREATERTHAN_OR_EQUALS:
                            ivlts.setLow(transformTest(lDate));
                            break;
                        case LESSTHAN_OR_EQUALS:
                            ivlts.setHigh(transformTest(hDate));
                            break;
                        case STARTS_AFTER:
                            ivlts.setLow(transformTest(hDate));
                            break;
                        case ENDS_BEFORE:
                            ivlts.setHigh(transformTest(lDate));
                            break;
                        case EQUAL:
                        case APPROXIMATE:
                            ivlts.setLow(transformTest(lDate));
                            ivlts.setHigh(transformTest(hDate));
                            ivlts.setCenter(transform(lDate));
                            break;
                        //case NOT_EQUAL:
                        //
                        default:
                            throw new InvalidRequestException("Date operation not supported.");
                    }


                }
            }

            parameterList.addLivingSubjectBirthTime(createBirthTime(ivlts));
        }

        // given, family -> livingSubjectName
        StringAndListParam given = parameters.getGiven();
        StringAndListParam family = parameters.getFamily();
        StringParam givenElem = given != null ? given.getValuesAsQueryTokens().get(0).getValuesAsQueryTokens().get(0) : null;
        StringParam familyElem = family != null ? family.getValuesAsQueryTokens().get(0).getValuesAsQueryTokens().get(0) : null;

        if (givenElem != null || familyElem != null) {
            EN name = new EN();

            if (familyElem != null) {
                name.addFamily(element(EnFamily.class, familyElem.getValue()));
                if (!familyElem.isExact()) name.setUse("SRCH");
            }
            if (givenElem != null) {
                name.addGiven(element(EnGiven.class, givenElem.getValue()));
                if (!givenElem.isExact()) name.setUse("SRCH");
            }
            parameterList.addLivingSubjectName(createLivingSubjectName(name));
        }

        // gender -> livingSubjectAdministrativeGender
        TokenParam gender = parameters.getGender();
        if (gender != null) {
            parameterList.addLivingSubjectAdministrativeGender(createAdministrativeGender(gender.getValue()));
        }

        // identifiers -> livingSubjectId or otherIDsScopingOrganization
        TokenAndListParam identifiers = parameters.getIdentifiers();
        if (identifiers != null) {
            for (TokenOrListParam idOr : identifiers.getValuesAsQueryTokens()) {
                for (TokenParam identifier : idOr.getValuesAsQueryTokens()) {
                    if (identifier.getValue() == null || identifier.getValue().length() == 0) {
                        parameterList.addOtherIDsScopingOrganization(createOtherIDsScopingOrganization(identifier.getSystem()));
                    } else {
                        parameterList.addLivingSubjectId(createSubjectId(identifier.getSystem(),
                                                                         identifier.getValue()));
                    }
                }
            }
        }

        // mothersMaidenName -> mothersMaidenName
        StringParam mmn = parameters.getMothersMaidenName();
        if (mmn != null) {
            parameterList.addMothersMaidenName(createMothersMaidenName(mmn.getValue()));
        }

        // telecom -> patientTelecom
        StringParam telecom = parameters.getTelecom();
        if (telecom != null) {
            parameterList.addPatientTelecom(createPatientTelecom(telecom.getValue()));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HL7V3Transformer.marshallMessage(PRPAIN201305UV02Type.class, out, resultMsg);
        String outArray = new String(out.toByteArray());
        return outArray;

    }

    public String idConverter(@Header(value = "FhirHttpUri") String fhirHttpUri) throws JAXBException {
        String uniqueId = fhirHttpUri.substring(fhirHttpUri.lastIndexOf("/") + 1);
        Iti78SearchParameters params = Iti78SearchParameters.builder()._id(new TokenParam(uniqueId)).build();
        return iti78ToIti47Converter(params);
    }

    public String fromMethodOutcome(MethodOutcome outcome) throws JAXBException {
        String uniqueId = outcome.getId().getIdPart();
        Iti78SearchParameters params = Iti78SearchParameters.builder()._id(new TokenParam(uniqueId)).build();
        return iti78ToIti47Converter(params);
    }

    public PRPAMT201306UV02LivingSubjectAdministrativeGender createAdministrativeGender(final String genderCode) {
        final var administrativeGender = new PRPAMT201306UV02LivingSubjectAdministrativeGender();
        switch (genderCode.toUpperCase()) {
            case "MALE":
                administrativeGender.addValue(new CE("M", "Male", "2.16.840.1.113883.12.1"));
                break;
            case "FEMALE":
                administrativeGender.addValue(new CE("F", "Female", "2.16.840.1.113883.12.1"));
                break;
            case "OTHER":
                administrativeGender.addValue(new CE("A", "Ambiguous", "2.16.840.1.113883.12.1"));
                break;
            case "UNKNOWN":
                administrativeGender.addValue(new CE("U", "Unknown", "2.16.840.1.113883.12.1"));
                break;
            default:
                throw new InvalidRequestException("Unknown gender query parameter value");
        }
        administrativeGender.setSemanticsText(ST("LivingSubject.administrativeGender"));
        return administrativeGender;
    }

    public PRPAMT201306UV02PatientAddress createAddress(final AD ad) {
        final var patientAddress = new PRPAMT201306UV02PatientAddress();
        patientAddress.addValue(ad);
        patientAddress.setSemanticsText(ST("Patient.addr"));
        return patientAddress;
    }

    public PRPAMT201306UV02LivingSubjectId createSubjectId(final String system,
                                                           final String value) {
        final var livingSubjectId = new PRPAMT201306UV02LivingSubjectId();
        livingSubjectId.addValue(new II(this.getScheme(system), value));
        livingSubjectId.setSemanticsText(ST("LivingSubject.id"));
        return livingSubjectId;
    }

    public PRPAMT201306UV02OtherIDsScopingOrganization createOtherIDsScopingOrganization(final String system) {
        final var otherIDsScopingOrganization = new PRPAMT201306UV02OtherIDsScopingOrganization();
        otherIDsScopingOrganization.addValue(new II(this.getScheme(system), null));
        otherIDsScopingOrganization.setSemanticsText(ST("OtherIDs.scopingOrganization.id"));
        return otherIDsScopingOrganization;
    }

    public PRPAMT201306UV02MothersMaidenName createMothersMaidenName(final String name) {
        final var mothersMaidenName = new PRPAMT201306UV02MothersMaidenName();
        final var mothersMaidenNamePN = new PN();
        mothersMaidenNamePN.addGiven(element(EnGiven.class, name));
        mothersMaidenName.addValue(mothersMaidenNamePN);
        mothersMaidenName.setSemanticsText(ST("Person.MothersMaidenName"));
        return mothersMaidenName;
    }

    public PRPAMT201306UV02PatientTelecom createPatientTelecom(final String telecom) {
        final var patientTelecom = new PRPAMT201306UV02PatientTelecom();
        final var tel = new TEL();
        tel.setValue(telecom);
        patientTelecom.addValue(tel);
        patientTelecom.setSemanticsText(ST("Patient.telecom"));
        return patientTelecom;
    }

    public PRPAMT201306UV02LivingSubjectBirthTime createBirthTime(final IVLTS ivlts) {
        final var livingSubjectBirthTime = new PRPAMT201306UV02LivingSubjectBirthTime();
        livingSubjectBirthTime.addValue(ivlts);
        livingSubjectBirthTime.setSemanticsText(ST("LivingSubject.birthTime"));
        return livingSubjectBirthTime;
    }

    public PRPAMT201306UV02LivingSubjectName createLivingSubjectName(final EN name) {
        final var livingSubjectName = new PRPAMT201306UV02LivingSubjectName();
        livingSubjectName.addValue(name);
        livingSubjectName.setSemanticsText(ST("LivingSubject.name"));
        return livingSubjectName;
    }
}
