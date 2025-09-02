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

package ch.bfh.ti.i4mi.mag.mhd;

import ca.uhn.fhir.rest.param.TokenParam;
import jakarta.annotation.Nullable;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * mapper for code systems to schemes
 *
 * @author alexander kreutz
 *
 */
@Service
public class SchemeMapper {

    public static final String PREFIX_URN_OID = "urn:oid:";
    public static final String PREFIX_URN_UUID = "urn:uuid:";

    // This file is responsible for mapping between FHIR and IHE code systems.em].
    public static final CodeSystemMapping DCM =
            new CodeSystemMapping("http://dicom.nema.org/resources/ontology/DCM", "1.2.840.10008.2.16.4");
    public static final CodeSystemMapping FORMAT_CODE =
            new CodeSystemMapping("http://ihe.net/fhir/ihe.formatcode.fhir/CodeSystem/formatcode",
                                  "1.3.6.1.4.1.19376.1.2.3");
    public static final CodeSystemMapping SNOMED_CT =
            new CodeSystemMapping("http://snomed.info/sct", "2.16.840.1.113883.6.96");
    public static final CodeSystemMapping SNOMED_CT_CH =
            new CodeSystemMapping("http://snomed.info/sct", "2.16.756.5.30.1.127.3.4");
    public static final String SNOMED_CT_CH_VERSION = "http://snomed.info/sct/2011000195101";

    // The list of all codes of the SNOMED CT Swiss extension that we care about.
    public static final List<String> SNOMED_CT_CH_CODES =
            List.of(
                    // classCode
                    "2171000195109",
                    // typeCode
                    "2161000195103",
                    "82291000195104",
                    // confidentialityCode
                    "1141000195107"
            );


    public Coding toFhirCoding(final Code code) {
        return this.toFhirCoding(code.getCode(), code.getSchemeName());
    }

    public Coding toFhirCoding(final Identifiable code) {
        return this.toFhirCoding(code.getId(), code.getAssigningAuthority().getUniversalId());
    }

    public Coding toFhirCoding(final String value,
                               final String system) {
        final var coding = new Coding();
        coding.setCode(value);
        coding.setSystem(this.toFhirSystem(system));
        if (SNOMED_CT_CH.fhirUrn.equals(system) && SNOMED_CT_CH_CODES.contains(value)) {
            coding.setVersion(SNOMED_CT_CH_VERSION);
        }
        return coding;
    }

    public CodeableConcept toFhirCodeableConcept(final Code code) {
        final var concept = new CodeableConcept();
        concept.addCoding(this.toFhirCoding(code));
        return concept;
    }

    public Identifier toFhirIdentifier(final Identifiable identifiable,
                                       final @Nullable Identifier.IdentifierUse use) {
        return this.toFhirIdentifier(
                identifiable.getAssigningAuthority().getUniversalId(),
                identifiable.getId(),
                use
        );
    }

    public Identifier toFhirIdentifier(final @Nullable String systemOid,
                                       final String value,
                                       final @Nullable Identifier.IdentifierUse use) {
        final var identifier = new Identifier();
        identifier.setValue(value);
        identifier.setUse(use);
        identifier.setSystem(this.toFhirSystem(systemOid));
        return identifier;
    }

    public Code toXdsCode(final Coding coding) {
        return new Code(
                coding.getCode(),
                new LocalizedString(coding.getDisplay()),
                this.toXdsSystem(coding.getSystem(), SNOMED_CT_CH_CODES.contains(coding.getCode()))
        );
    }

    public Identifiable toXdsIdentifiable(final Coding coding) {
        return this.toXdsIdentifiable(coding.getCode(), coding.getSystem());
    }

    public Identifiable toXdsIdentifiable(final Identifier identifier) {
        return this.toXdsIdentifiable(identifier.getValue(), identifier.getSystem());
    }

    public Identifiable toXdsIdentifiable(final String value,
                                          final String system) {
        return new Identifiable(
                value,
                new AssigningAuthority(this.toXdsSystem(system, SNOMED_CT_CH_CODES.contains(value)))
        );
    }

    public Code toXdsCode(final TokenParam param) {
        return new Code(
                param.getValue(),
                null,
                this.toXdsSystem(param.getSystem(), SNOMED_CT_CH_CODES.contains(param.getValue()))
        );
    }

    public ReferenceId toXdsReferenceId(final TokenParam param) {
        return new ReferenceId(
                param.getValue(),
                null,
                this.toXdsSystem(param.getSystem(), SNOMED_CT_CH_CODES.contains(param.getValue()))
        );
    }

    @Nullable
    private String toFhirSystem(final @Nullable String xdsSystem) {
        if (SNOMED_CT.iheOid.equals(xdsSystem)) {
            return SNOMED_CT.fhirUrn;
        } else if (SNOMED_CT_CH.iheOid.equals(xdsSystem)) {
            return SNOMED_CT_CH.fhirUrn;
        } else if (DCM.iheOid.equals(xdsSystem)) {
            return DCM.fhirUrn;
        } else if (FORMAT_CODE.iheOid.equals(xdsSystem)) {
            return FORMAT_CODE.fhirUrn;
        } else if (this.isOid(xdsSystem)) {
            return PREFIX_URN_OID + xdsSystem;
        } else if (this.isUrl(xdsSystem)) {
            return xdsSystem;
        } else {
            return null;
        }
    }

    @Nullable
    private String toXdsSystem(final @Nullable String fhirSystem,
                               final boolean swissSctDetected) {
        if (SNOMED_CT_CH.fhirUrn.equals(fhirSystem) && swissSctDetected) {
            return SNOMED_CT_CH.iheOid;
        } else if (SNOMED_CT.fhirUrn.equals(fhirSystem)) {
            return SNOMED_CT.iheOid;
        } else if (DCM.fhirUrn.equals(fhirSystem)) {
            return DCM.iheOid;
        } else if (FORMAT_CODE.fhirUrn.equals(fhirSystem)) {
            return FORMAT_CODE.iheOid;
        } else if (fhirSystem == null) {
            return null;
        } else if (fhirSystem.startsWith(PREFIX_URN_OID)) {
            return fhirSystem.substring(PREFIX_URN_OID.length());
        } else if (fhirSystem.startsWith(PREFIX_URN_UUID)) {
            return fhirSystem.substring(PREFIX_URN_UUID.length());
        } else {
            return fhirSystem;
        }
    }

    private boolean isOid(final String maybeOid) {
        return maybeOid != null && maybeOid.matches("[0-2](\\.(0|[1-9][0-9]*))+");
    }

    private boolean isUrl(final String maybeUrl) {
        return maybeUrl != null && (maybeUrl.startsWith("http://") || maybeUrl.startsWith("https://"));
    }

    public record CodeSystemMapping(String fhirUrn,
                                    String iheOid) {
    }
}
