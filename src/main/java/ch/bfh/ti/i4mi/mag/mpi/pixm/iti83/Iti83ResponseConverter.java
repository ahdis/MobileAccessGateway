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

package ch.bfh.ti.i4mi.mag.mpi.pixm.iti83;

import ca.uhn.fhir.rest.server.exceptions.*;
import ch.bfh.ti.i4mi.mag.mpi.pmir.BasePMIRResponseConverter;
import jakarta.xml.bind.JAXBException;
import lombok.val;
import net.ihe.gazelle.hl7v3.datatypes.BIN;
import net.ihe.gazelle.hl7v3.datatypes.II;
import net.ihe.gazelle.hl7v3.mccimt000300UV01.MCCIMT000300UV01Acknowledgement;
import net.ihe.gazelle.hl7v3.mccimt000300UV01.MCCIMT000300UV01AcknowledgementDetail;
import net.ihe.gazelle.hl7v3.prpain201310UV02.PRPAIN201310UV02MFMIMT700711UV01ControlActProcess;
import net.ihe.gazelle.hl7v3.prpain201310UV02.PRPAIN201310UV02MFMIMT700711UV01RegistrationEvent;
import net.ihe.gazelle.hl7v3.prpain201310UV02.PRPAIN201310UV02MFMIMT700711UV01Subject1;
import net.ihe.gazelle.hl7v3.prpain201310UV02.PRPAIN201310UV02MFMIMT700711UV01Subject2;
import net.ihe.gazelle.hl7v3.prpain201310UV02.PRPAIN201310UV02Type;
import net.ihe.gazelle.hl7v3.prpamt201307UV02.PRPAMT201307UV02DataSource;
import net.ihe.gazelle.hl7v3transformer.HL7V3Transformer;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.openehealth.ipf.commons.ihe.fhir.translation.ToFhirTranslator;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.bfh.ti.i4mi.mag.mpi.common.FhirExceptions.*;
import static ch.bfh.ti.i4mi.mag.mpi.common.Hl7v3Mappers.verifyAck;

/**
 * ITI-83 from ITI-45 response converter
 *
 * @author alexander kreutz
 */
@Component
public class Iti83ResponseConverter extends BasePMIRResponseConverter implements ToFhirTranslator<byte[]> {


    public Parameters translateToFhir(final byte[] input,
                                      final Map<String, Object> parameters) {
        try {
            return this.doTranslate(input);
        } catch (final BaseServerResponseException controlledException) {
            throw controlledException;
        } catch (final JAXBException parsingException) {
            throw new InvalidRequestException("Failed parsing ITI-45 response", parsingException);
        } catch (final Exception otherException) {
            throw new InvalidRequestException("Unexpected exception during ITI-45 response processing", otherException);
        }
    }

    private Parameters doTranslate(final byte[] input) throws Exception {
        final PRPAIN201310UV02Type pixResponse = HL7V3Transformer.unmarshallMessage(PRPAIN201310UV02Type.class,
                                                                                    new ByteArrayInputStream(input));
        final PRPAIN201310UV02MFMIMT700711UV01ControlActProcess controlAct = pixResponse.getControlActProcess();

        // OK NF AE
        verifyAck(controlAct.getQueryAck().getQueryResponseCode(), pixResponse.getAcknowledgement(), "ITI-45");

        // Find requested target systems from the query
        final var requestedTargetSystems = controlAct.getQueryByParameter().getParameterList().getDataSource()
                .stream()
                .map(PRPAMT201307UV02DataSource::getValue)
                .flatMap(List::stream)
                .map(II::getRoot)
                .collect(Collectors.toUnmodifiableSet());


        final var response = new Parameters();

        // Iterate over all patients and their IDs, only keep those whose system was requested
        controlAct.getSubject().parallelStream()
                .map(PRPAIN201310UV02MFMIMT700711UV01Subject1::getRegistrationEvent)
                .map(PRPAIN201310UV02MFMIMT700711UV01RegistrationEvent::getSubject1)
                .map(PRPAIN201310UV02MFMIMT700711UV01Subject2::getPatient)
                .flatMap(patient -> Stream.concat(
                        patient.getId().stream(),
                        patient.getPatientPerson().getAsOtherIDs().stream().flatMap(otherId -> otherId.getId().stream())
                ))
                .forEach(ii -> {
                    if (requestedTargetSystems.contains(ii.getRoot())) {
                        response.addParameter()
                                .setName("targetIdentifier")
                                .setValue(new Identifier().setSystem("urn:oid:" + ii.getRoot()).setValue(ii.getExtension()));
                    }
                });

        return response;
    }
}
