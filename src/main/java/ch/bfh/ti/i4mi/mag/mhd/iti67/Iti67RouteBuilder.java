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

package ch.bfh.ti.i4mi.mag.mhd.iti67;

import ch.bfh.ti.i4mi.mag.common.MagRouteBuilder;
import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import ch.bfh.ti.i4mi.mag.config.props.MagXdsProps;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import org.apache.camel.builder.PredicateBuilder;
import org.hl7.fhir.r4.model.DocumentReference;
import org.openehealth.ipf.commons.ihe.fhir.Constants;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.lcm.SubmitObjectsRequest;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

/**
 * IHE MHD: Find Document References [ITI-67] for Document Responder https://oehf.github.io/ipf-docs/docs/ihe/iti67/
 */
@Component
@ConditionalOnProperty({"mag.xds.iti-18", "mag.xds.iti-57"})
class Iti67RouteBuilder extends MagRouteBuilder {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Iti67RouteBuilder.class);
    private final MagXdsProps xdsProps;
    private final Iti67ResponseConverter iti67ResponseConverter;
    private final Iti67RequestUpdateConverter iti67RequestUpdateConverter;
    private final Iti67FromIti57ResponseConverter iti67FromIti57ResponseConverter;

    public Iti67RouteBuilder(final MagProps magProps,
                             final Iti67ResponseConverter iti67ResponseConverter,
                             final Iti67RequestUpdateConverter iti67RequestUpdateConverter,
                             final Iti67FromIti57ResponseConverter iti67FromIti57ResponseConverter) {
        super(magProps);
        this.xdsProps = magProps.getXds();
        this.iti67ResponseConverter = iti67ResponseConverter;
        this.iti67RequestUpdateConverter = iti67RequestUpdateConverter;
        this.iti67FromIti57ResponseConverter = iti67FromIti57ResponseConverter;
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti67RouteBuilder configure");
        final String metadataQueryEndpoint = this.buildOutgoingEndpoint("xds-iti18",
                                                                        this.xdsProps.getIti18(),
                                                                        this.xdsProps.isHttps());
        final String metadataUpdateEndpoint = this.buildOutgoingEndpoint("xds-iti57",
                                                                         this.xdsProps.getIti57(),
                                                                         this.xdsProps.isHttps());

        from("mhd-iti67:find-document-references?audit=false")
                .routeId("in-mhd-iti67")
                .errorHandler(noErrorHandler())
                //.process(RequestHeadersForwarder.checkAuthorization(this.xdsProps.isChMhdConstraints()))
                .process(RequestHeadersForwarder.forward())
                .choice()
                    // It is a search request with parameters
                    .when(header(Constants.FHIR_REQUEST_PARAMETERS).isNotNull())
                        .bean(Utils.class, "searchParameterToBody")
                        .bean(Iti67RequestConverter.class)
                        .to(metadataQueryEndpoint)
                        // TODO: also forward to XCA-38 and merge results
                        .process(TraceparentHandler.updateHeaderForFhir())
                        .process(translateToFhir(iti67ResponseConverter, QueryResponse.class))
                    // It is a read request for a specific resource. Disabled for now
                    /*
                    .when(PredicateBuilder.and(header("FhirHttpUri").isNotNull(),
                                           header("FhirHttpMethod").isEqualTo("GET")))
                        .bean(IdRequestConverter.class)
                        .to(metadataQueryEndpoint)
                        .process(TraceparentHandler.updateHeaderForFhir())
                        .process(translateToFhir(iti67ResponseConverter, QueryResponse.class))
                     */
                    // It is an update request: CH:MHD-1
                    .when(PredicateBuilder.and(header("FhirHttpUri").isNotNull(),
                                           header("FhirHttpMethod").isEqualTo("PUT")))
                        .process(exchange -> {
                            DocumentReference documentReference = exchange.getIn().getMandatoryBody(DocumentReference.class);
                            SubmitObjectsRequest submitObjectsRequest = iti67RequestUpdateConverter.createMetadataUpdateRequest(
                                    documentReference);
                            exchange.getMessage().setBody(submitObjectsRequest);
                        })
                        .to(metadataUpdateEndpoint)
                        .process(TraceparentHandler.updateHeaderForFhir())
                        .process(translateToFhir(iti67FromIti57ResponseConverter, Response.class))
                    // It is a delete request. Disabled for now
                    /*
                    .when(PredicateBuilder.and(header("FhirHttpUri").isNotNull(),
                                           header("FhirHttpMethod").isEqualTo("DELETE")))
                        .process(exchange -> {
                            exchange.setProperty("DOCUMENT_ENTRY_LOGICAL_ID",
                                                 IdRequestConverter.extractId(exchange.getIn().getHeader("FhirHttpUri",
                                                                                                         String.class)));
                        })
                        .bean(IdRequestConverter.class)
                        .to(metadataQueryEndpoint)
                        .process(TraceparentHandler.updateHeaderForFhir())
                        .process(exchange -> {
                            QueryResponse queryResponse = exchange.getIn().getMandatoryBody(QueryResponse.class);
                            if (queryResponse.getStatus() != Status.SUCCESS) {
                                iti67FromIti57ResponseConverter.processError(queryResponse);
                            }
                            if (queryResponse.getDocumentEntries().isEmpty()) {
                                throw new ResourceNotFoundException(exchange.getProperty("DOCUMENT_ENTRY_LOGICAL_ID",
                                                                                         String.class));
                            }
                            if (queryResponse.getDocumentEntries().size() > 1) {
                                throw new InternalErrorException("Expected at most one Document Entry, got " + queryResponse.getDocumentEntries().size());
                            }

                            DocumentEntry documentEntry = queryResponse.getDocumentEntries().get(0);
                            if (documentEntry.getExtraMetadata() == null) {
                                documentEntry.setExtraMetadata(new HashMap<>());
                            }
                            documentEntry.getExtraMetadata().put(MagConstants.XdsExtraMetadataSlotNames.CH_DELETION_STATUS,
                                                                 List.of(MagConstants.DeletionStatuses.REQUESTED));
                            if (documentEntry.getLogicalUuid() == null) {
                                documentEntry.setLogicalUuid(documentEntry.getEntryUuid());
                            }
                            documentEntry.assignEntryUuid();
                            if (documentEntry.getVersion() == null) {
                                documentEntry.setVersion(new Version("1"));
                            }

                            SubmissionSet submissionSet = iti67RequestUpdateConverter.createSubmissionSet();
                            SubmitObjectsRequest updateRequest = iti67RequestUpdateConverter.createMetadataUpdateRequest(
                                    submissionSet,
                                    documentEntry);
                            exchange.getMessage().setBody(updateRequest);
                            log.debug("Prepared document metadata update request");
                        })
                        .choice()
                            .when(exchange -> exchange.getIn().getBody() instanceof SubmitObjectsRequest)
                                .to(metadataUpdateEndpoint)
                                .process(TraceparentHandler.updateHeaderForFhir())
                                .process(translateToFhir(new Iti67FromIti57ResponseConverter(), Response.class))
                        .endChoice()
                    .end()
                     */
                .end();
    }
}
