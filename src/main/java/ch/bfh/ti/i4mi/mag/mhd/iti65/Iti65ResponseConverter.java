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

package ch.bfh.ti.i4mi.mag.mhd.iti65;

import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import ch.bfh.ti.i4mi.mag.config.props.MagXdsProps;
import ch.bfh.ti.i4mi.mag.mhd.BaseResponseConverter;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.ListResource;
import org.openehealth.ipf.commons.ihe.fhir.translation.ToFhirTranslator;
import org.openehealth.ipf.commons.ihe.xds.core.requests.ProvideAndRegisterDocumentSet;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * ITI-65 from ITI-41 response converter
 *
 * @author alexander kreutz
 *
 */
@Component
public class Iti65ResponseConverter extends BaseResponseConverter implements ToFhirTranslator<Response> {

    private final MagProps magProps;
    private final MagXdsProps xdsProps;

    public Iti65ResponseConverter(final MagProps magProps) {
        this.magProps = magProps;
        this.xdsProps = magProps.getXds();
    }

    /**
     * convert ITI-41 response to ITI-65 response
     */
    @Override
    public Object translateToFhir(Response input, Map<String, Object> parameters) {

        String entryUuid = null;
        if (input.getStatus().equals(Status.SUCCESS)) {
            Bundle responseBundle = new Bundle();
            ProvideAndRegisterDocumentSet prb = (ProvideAndRegisterDocumentSet) parameters.get(
                    "ProvideAndRegisterDocumentSet");
            entryUuid = Iti65RequestConverter.noPrefix(prb.getDocuments().get(0).getDocumentEntry().getEntryUuid());
            Bundle requestBundle = (Bundle) parameters.get("BundleRequest");
            for (Bundle.BundleEntryComponent requestEntry : requestBundle.getEntry()) {
                Bundle.BundleEntryResponseComponent response = new Bundle.BundleEntryResponseComponent()
                        .setStatus("201 Created")
                        .setLastModified(new Date());
                if (requestEntry.getResource() instanceof Binary) {
                    String uniqueId = (String) requestEntry.getResource().getUserData("masterIdentifier");
                    response.setLocation(this.xdsProps.getRetrieve().getUrl() + "?uniqueId=" + uniqueId
                                                 + "&repositoryUniqueId=" + this.xdsProps.getRepositoryUniqueId());
                } else if (requestEntry.getResource() instanceof ListResource) {
                    response.setLocation(this.magProps.getBaseUrl() + "/fhir/List/" + Iti65RequestConverter.noPrefix(prb.getSubmissionSet().getEntryUuid()));
                } else if (requestEntry.getResource() instanceof DocumentReference) {
                    response.setLocation(this.magProps.getBaseUrl() + "/fhir/DocumentReference/" + entryUuid);
                }
                responseBundle.addEntry()
                        .setResponse(response);

            }
            return responseBundle;
        } else {
            processError(input);
            return null;
        }
    }

}
