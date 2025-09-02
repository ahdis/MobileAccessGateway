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

package ch.bfh.ti.i4mi.mag.mpi.pmir.iti93;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.config.props.MagMpiProps;
import ch.bfh.ti.i4mi.mag.mhd.SchemeMapper;
import jakarta.xml.bind.JAXBException;
import org.apache.camel.Body;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;

import java.util.HashMap;
import java.util.Map;

/**
 * ITI-93 to ITI-44 request converter
 *
 * @author alexander kreutz
 *
 */
public class Iti93RequestConverter extends Iti93MergeRequestConverter {

    public Iti93RequestConverter(final SchemeMapper schemeMapper,
                                 final MagMpiProps mpiProps) {
        super(schemeMapper, mpiProps);
    }

    /**
     * convert ITI-93 to ITI-44 request
     *
     * @param requestBundle
     * @return
     * @throws JAXBException
     */
    public String iti93ToIti44Converter(@Body Bundle requestBundle) throws JAXBException {
        if (requestBundle.getType() != BundleType.MESSAGE)
            throw new InvalidRequestException("Bundle type must be message");

        BundleEntryComponent headerComponent = requestBundle.getEntryFirstRep();
        if (headerComponent == null) throw new InvalidRequestException("First bundle entry must be MessageHeader.");
        Resource headerResource = headerComponent.getResource();
        if (headerResource == null || !(headerResource instanceof MessageHeader))
            throw new InvalidRequestException("First bundle entry must be MessageHeader.");
        MessageHeader header = (MessageHeader) headerResource;

        if (!("urn:ihe:iti:pmir:2019:patient-feed".equals(header.getEventUriType().getValue())))
            throw new InvalidRequestException("Wrong eventUri");

        // use nested bundle
        if (requestBundle.getEntry().size() > 1 && requestBundle.getEntry().get(1).getResource() instanceof Bundle) {
            requestBundle = (Bundle) requestBundle.getEntry().get(1).getResource();
            if (requestBundle.getType() != BundleType.HISTORY)
                throw new InvalidRequestException("Nested bundle type must be history");
        }
        BundleEntryComponent firstEntry = null;
        Map<String, BundleEntryComponent> entriesByReference = new HashMap<String, BundleEntryComponent>();
        for (BundleEntryComponent entry : requestBundle.getEntry()) {
            if (entry.getResource() instanceof Patient) {

                entriesByReference.put("Patient/" + entry.getResource().getIdElement().getIdPart(), entry);
                if (firstEntry == null) firstEntry = entry;
            }
        }


        BundleEntryComponent entry = firstEntry;
        HTTPVerb method = entry.getRequest().getMethod();
        if (method == null) throw new InvalidRequestException("HTTP verb missing in Bundle for Patient resource.");

        if (method.equals(HTTPVerb.POST)) {
            return doCreate(header, entriesByReference);
        } else if (method.equals(HTTPVerb.PUT)) {
            if (((Patient) entry.getResource()).getActive() == false) {
                return doMerge(header, entriesByReference);
            } else {
                return doUpdate(header, entriesByReference);
            }
        } else if (method.equals(HTTPVerb.DELETE)) {
            return doUpdate(header, entriesByReference);
        }

        throw new InvalidRequestException("Cannot handle request");
    }


}
