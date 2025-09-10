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

package ch.bfh.ti.i4mi.mag.mhd.iti68;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.BaseRequestConverter;
import ch.bfh.ti.i4mi.mag.mhd.SchemeMapper;
import org.apache.camel.Headers;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.requests.RetrieveDocumentSet;

import java.util.Map;

/**
 * ITI-68 to ITI-43 request converter
 * @author alexander kreutz
 */
public class Iti68RequestConverter extends BaseRequestConverter {

    public Iti68RequestConverter(final SchemeMapper schemeMapper) {
        super(schemeMapper);
    }

    public static RetrieveDocumentSet queryParameterToRetrieveDocumentSet(final @Headers Map<String, Object> parameters) {
        final var documentEntry = new DocumentEntry();

        if (parameters.get("repositoryUniqueId") instanceof final String repositoryUniqueId) {
            documentEntry.setRepositoryUniqueId(repositoryUniqueId);
        } else {
            throw new InvalidRequestException("Missing required parameter: repositoryUniqueId");
        }

        if (parameters.get("homeCommunityId") instanceof final String homeCommunityId) {
            documentEntry.setHomeCommunityId(homeCommunityId);
        } else {
            throw new InvalidRequestException("Missing required parameter: homeCommunityId");
        }

        if (parameters.get("uniqueId") instanceof final String uniqueId) {
            documentEntry.setUniqueId(uniqueId);
        } else {
            throw new InvalidRequestException("Missing required parameter: uniqueId");
        }

        final var retrieveDocumentSet = new RetrieveDocumentSet();
        retrieveDocumentSet.addReferenceTo(documentEntry);
        return retrieveDocumentSet;
    }
}
