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

import java.util.Map;

import org.apache.camel.Headers;
import org.apache.commons.lang3.StringUtils;
import org.openehealth.ipf.commons.ihe.xds.core.requests.DocumentReference;
import org.openehealth.ipf.commons.ihe.xds.core.requests.RetrieveDocumentSet;

import ch.bfh.ti.i4mi.mag.BaseRequestConverter;
import org.springframework.stereotype.Component;

/**
 * ITI-68 to ITI-43 request converter
 * @author alexander kreutz
 *
 */
@Component
public class Iti68RequestConverter extends BaseRequestConverter {

    public static final String HOME_COMMUNITY_ID_URL_PARAM      = "homeCommunityId";
    public static final String REPOSITORY_UNIQUE_ID_URL_PARAM   = "repositoryUniqueId";
    public static final String DOCUMENT_UNIQUE_ID_URL_PARAM     = "uniqueId";

    public RetrieveDocumentSet queryParameterToRetrieveDocumentSet(@Headers Map<String, Object> parameters) {

        RetrieveDocumentSet retrieveDocumentSet = new RetrieveDocumentSet();

        String homeCommunityId = (String) parameters.get(HOME_COMMUNITY_ID_URL_PARAM);
        retrieveDocumentSet.getDocuments().add(new DocumentReference(
                (String) parameters.get(REPOSITORY_UNIQUE_ID_URL_PARAM),
                (String) parameters.get(DOCUMENT_UNIQUE_ID_URL_PARAM),
                (StringUtils.isBlank(homeCommunityId) || config.getHomeCommunity().equalsIgnoreCase(homeCommunityId)) ? null : homeCommunityId));

        return retrieveDocumentSet;
    }

}
