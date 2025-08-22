/*
 * Copyright 2015 the original author or authors.
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

package org.openehealth.ipf.commons.ihe.fhir.iti119;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.api.annotation.Extension;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.util.ElementUtil;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;

/**
 * Patient as defined by the PDQm specification
 *
 * @author Christian Ohr
 * @since 5.0
 */
@ResourceDef(name = "Patient", id = "pdqm", profile = "https://profiles.ihe.net/ITI/PDQm/StructureDefinition/IHE.PDQm.MatchInputPatient")
public class PdqmMatchInputPatient extends Patient {
    public static final String MOTHERS_MAIDEN_NAME_EXT = "http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName";


    @Child(name = "mothersMaidenName")
    @Extension(url = MOTHERS_MAIDEN_NAME_EXT, definedLocally = false)
    @Description(shortDefinition = "Mother's maiden name of a patient")
    private HumanName mothersMaidenName;

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && ElementUtil.isEmpty(mothersMaidenName);
    }

    public HumanName getMothersMaidenName() {
        if (mothersMaidenName == null) {
            mothersMaidenName = new HumanName();
        }
        return mothersMaidenName;
    }

    public void setMothersMaidenName(HumanName mothersMaidenName) {
        this.mothersMaidenName = mothersMaidenName;
    }

    public boolean hasMothersMaidenName() {
        return this.mothersMaidenName != null && !this.mothersMaidenName.isEmpty();
    }

}
