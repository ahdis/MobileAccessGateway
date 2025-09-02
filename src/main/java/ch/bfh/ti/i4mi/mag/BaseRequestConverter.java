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

package ch.bfh.ti.i4mi.mag;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ch.bfh.ti.i4mi.mag.mhd.SchemeMapper;
import net.ihe.gazelle.hl7v3.datatypes.ST;

/**
 * Base class for request converters
 *
 */
public class BaseRequestConverter {

    protected final SchemeMapper schemeMapper;

    public BaseRequestConverter(final SchemeMapper schemeMapper) {
        this.schemeMapper = schemeMapper;
    }

	public Timestamp timestampFromDateParam(DateParam dateParam) {
		if (dateParam == null)
			return null;
		String dateString = dateParam.getValueAsString();
		dateString = dateString.replaceAll("-", "");
		return Timestamp.fromHL7(dateString);
	}

	public Code codeFromToken(TokenParam param) {
		return this.schemeMapper.toXdsCode(param);
	}

    @Nullable
	public List<Code> codesFromTokens(final TokenOrListParam params) {
		if (params == null)
			return null;
        final var tokens = params.getValuesAsQueryTokens();
		final List<Code> codes = new ArrayList<>(tokens.size());
		for (final TokenParam token : tokens) {
			codes.add(codeFromToken(token));
		}
		return codes;
	}

	// TODO is this the correct mapping for URIs?
    @Nullable
	public List<String> urisFromTokens(final TokenOrListParam params) {
		if (params == null)
			return null;
        final var tokens = params.getValuesAsQueryTokens();
        final List<String> result = new ArrayList<>(tokens.size());
		for (final TokenParam token : tokens) {
			result.add(token.getValue());
		}
		return result;
	}

	public ST ST(String text) {
		ST semanticsText = new ST();
		semanticsText.addMixed(text);
		return semanticsText;
	}

	public Identifiable transformReference(String targetRef) {
		if (targetRef == null) return null;

		MultiValueMap<String, String> vals = UriComponentsBuilder.fromUriString(targetRef).build().getQueryParams();
		if (vals.containsKey("identifier")) {
			String ids = vals.getFirst("identifier");
			if (ids == null) return null;
			String[] identifier = ids.split("\\|");
			if (identifier.length == 2) {
				return this.schemeMapper.toXdsIdentifiable(identifier[1], identifier[0]);
			}
		}
		return null;
	}

	private static volatile long currentId = System.currentTimeMillis();

	public String uniqueId() {
		return Long.toString(currentId++);
	}

	public Extension getExtensionByUrl(DomainResource resource,String url) {
		if (url != null && resource != null) {
		    Extension ext = resource.getExtensionByUrl(url);
		    if (ext != null) return ext;
		    if (url.startsWith("https://")) ext = resource.getExtensionByUrl(url.replace("https://", "http://"));
		    return ext;
		}
		return null;
	}
}
