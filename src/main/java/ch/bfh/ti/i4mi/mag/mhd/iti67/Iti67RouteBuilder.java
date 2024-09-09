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

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ch.bfh.ti.i4mi.mag.MagConstants;
import ch.bfh.ti.i4mi.mag.MagRouteBuilder;
import ch.bfh.ti.i4mi.mag.mhd.XdsDispatchingUtils;
import org.apache.camel.Message;
import org.hl7.fhir.r4.model.DocumentReference;
import org.openehealth.ipf.commons.ihe.fhir.Constants;
import org.openehealth.ipf.commons.ihe.fhir.iti67_v401.Iti67SearchParameters;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.*;
import org.openehealth.ipf.commons.ihe.xds.core.requests.QueryRegistry;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.lcm.SubmitObjectsRequest;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.query.AdhocQueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import ch.bfh.ti.i4mi.mag.xua.AuthTokenConverter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;

/**
 * IHE MHD: Find Document References [ITI-67] for Document Responder
 * https://oehf.github.io/ipf-docs/docs/ihe/iti67/
 */
@Slf4j
@Component
@ConditionalOnProperty({"mag.xds.iti-18.url", "mag.xds.iti-57.url"})
class Iti67RouteBuilder extends MagRouteBuilder {

    private static final String PROP_TARGET = Iti67RouteBuilder.class.getName() + ".target";
    private static final String PROP_DISPATCHING_UTILS = Iti67RouteBuilder.class.getName() + ".dispatching-utils";
    private static final String PROP_DOCUMENT_ENTRY_LID = Iti67RouteBuilder.class.getName() + ".doc-entry-lid";

    private final Iti67RequestConverter iti67RequestConverter;
    private final Iti67ResponseConverter iti67ResponseConverter;
    private final Iti67RequestUpdateConverter iti67RequestUpdateConverter;
    private final Iti67FromIti57ResponseConverter iti67FromIti57ResponseConverter;

    @Autowired
    public Iti67RouteBuilder(Config config,
                             Iti67RequestConverter iti67RequestConverter,
                             Iti67ResponseConverter iti67ResponseConverter,
                             Iti67RequestUpdateConverter iti67RequestUpdateConverter,
                             Iti67FromIti57ResponseConverter iti67FromIti57ResponseConverter)
    {
        super(config);
        log.debug("Iti67RouteBuilder initialized");
        this.iti67RequestConverter = iti67RequestConverter;
        this.iti67ResponseConverter = iti67ResponseConverter;
        this.iti67RequestUpdateConverter = iti67RequestUpdateConverter;
        this.iti67FromIti57ResponseConverter = iti67FromIti57ResponseConverter;
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti67RouteBuilder configure");
        final String metadataQueryEndpoint = createSoapEndpointUri("xds-iti18", this.config.getIti18HostUrl());
        final String metadataQueryEndpointInitGw = createSoapEndpointUri("xds-iti18", this.config.getIti18HostUrlInitGw());
        final String metadataUpdateEndpoint = createSoapEndpointUri("xds-iti57", this.config.getIti57HostUrl());

        from("mhd-iti67-v401:translation?audit=true&auditContext=#myAuditContext")
                .routeId("mdh-documentreference-adapter")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                .process(AuthTokenConverter.addWsHeader())
                .process(exchange -> {
                    Message m = exchange.getIn();
                    String target;
                    if (m.getHeader(Constants.FHIR_REQUEST_PARAMETERS) != null) {
                        target = "search";
                    } else if (m.getHeader(Constants.HTTP_URI) != null) {
                        target = exchange.getIn().getHeader(Constants.HTTP_METHOD, String.class).toLowerCase();
                    } else {
                        throw new Exception("Have no idea what to do");
                    }
                    exchange.setProperty(PROP_TARGET, "direct:iti67-handle-" + target);
                })
                .recipientList().exchangeProperty(PROP_TARGET);


        fromDirect("iti67-handle-search")
                .process(exchange -> {
                    Iti67SearchParameters iti67Parameters = Utils.searchParameterToBody(exchange.getIn().getHeaders());
                    QueryRegistry iti18Request = iti67RequestConverter.searchParameterIti67ToFindDocumentsQuery(iti67Parameters);
                    XdsDispatchingUtils dispatchingUtils = new XdsDispatchingUtils(config.getHomeCommunity());
                    exchange.setProperty(PROP_DISPATCHING_UTILS, dispatchingUtils);
                    dispatchingUtils.handleIti18Request(iti18Request);
                })
                .choice()
                    .when(exchange -> exchange.getProperty(PROP_DISPATCHING_UTILS, XdsDispatchingUtils.class).getIti18RequestForDocumentRegistry() != null)
                        .to("direct:send-iti18-request-to-registry")
                    .otherwise()
                        .to("direct:send-iti18-request-to-ig")
                    .end()
                .process(translateToFhir(iti67ResponseConverter, QueryResponse.class));


        fromDirect("send-iti18-request-to-registry")
                .process(exchange -> {
                    exchange.getMessage().setBody(exchange.getProperty(PROP_DISPATCHING_UTILS, XdsDispatchingUtils.class).getIti18RequestForDocumentRegistry());
                })
                .to(metadataQueryEndpoint)
                .process(exchange -> {
                    AdhocQueryResponse iti18Response = exchange.getIn().getMandatoryBody(AdhocQueryResponse.class);
                    XdsDispatchingUtils dispatchingUtils = exchange.getProperty(PROP_DISPATCHING_UTILS, XdsDispatchingUtils.class);
                    dispatchingUtils.handleDocumentRegistryIti18Response(iti18Response);
                })
                .choice()
                    .when(exchange -> exchange.getProperty(PROP_DISPATCHING_UTILS, XdsDispatchingUtils.class).getIti18RequestForInitiatingGateway() != null)
                        .to("direct:send-iti18-request-to-ig");


        fromDirect("send-iti18-request-to-ig")
                .process(exchange -> {
                    exchange.getMessage().setBody(exchange.getProperty(PROP_DISPATCHING_UTILS, XdsDispatchingUtils.class).getIti18RequestForInitiatingGateway());
                })
                .to(metadataQueryEndpointInitGw)
                .process(exchange -> {
                    AdhocQueryResponse iti18Response = exchange.getIn().getMandatoryBody(AdhocQueryResponse.class);
                    XdsDispatchingUtils dispatchingUtils = exchange.getProperty(PROP_DISPATCHING_UTILS, XdsDispatchingUtils.class);
                    dispatchingUtils.handleInitiatingGatewayIti18Response(iti18Response);
                    exchange.getMessage().setBody(dispatchingUtils.getAggregatedIti18Response());
                });


        fromDirect("iti67-handle-get")
                .bean(IdRequestConverter.class)
                .to(metadataQueryEndpoint)
                .process(translateToFhir(iti67ResponseConverter, QueryResponse.class));


        fromDirect("iti67-handle-put")
                .process(exchange -> {
                    DocumentReference documentReference = exchange.getIn().getMandatoryBody(DocumentReference.class);
                    SubmitObjectsRequest submitObjectsRequest = iti67RequestUpdateConverter.createMetadataUpdateRequest(documentReference);
                    exchange.getMessage().setBody(submitObjectsRequest);
                })
                .to(metadataUpdateEndpoint)
                .process(translateToFhir(iti67FromIti57ResponseConverter, Response.class));


        fromDirect("iti67-handle-delete")
                .process(exchange -> {
                    exchange.setProperty(PROP_DOCUMENT_ENTRY_LID, IdRequestConverter.extractId(exchange.getIn().getHeader(Constants.HTTP_URI, String.class)));
                })
                .bean(IdRequestConverter.class)
                .to(metadataQueryEndpoint)
                .process(exchange -> {
                    QueryResponse queryResponse = exchange.getIn().getMandatoryBody(QueryResponse.class);
                    if (queryResponse.getStatus() != Status.SUCCESS) {
                        iti67FromIti57ResponseConverter.processError(queryResponse);
                    }
                    if (queryResponse.getDocumentEntries().isEmpty()) {
                        throw new ResourceNotFoundException(exchange.getProperty(PROP_DOCUMENT_ENTRY_LID, String.class));
                    }
                    if (queryResponse.getDocumentEntries().size() > 1) {
                        throw new InternalErrorException("Expected at most one DocumentEntry, got " + queryResponse.getDocumentEntries().size());
                    }

                    DocumentEntry documentEntry = queryResponse.getDocumentEntries().get(0);
                    if (documentEntry.getExtraMetadata() == null) {
                        documentEntry.setExtraMetadata(new HashMap<>());
                    }
                    documentEntry.getExtraMetadata().put(MagConstants.XdsExtraMetadataSlotNames.CH_DELETION_STATUS, List.of(MagConstants.DeletionStatuses.REQUESTED));
                    if (documentEntry.getLogicalUuid() == null) {
                        documentEntry.setLogicalUuid(documentEntry.getEntryUuid());
                    }
                    documentEntry.assignEntryUuid();
                    if (documentEntry.getVersion() == null) {
                        documentEntry.setVersion(new Version("1"));
                    }

                    SubmissionSet submissionSet = iti67RequestUpdateConverter.createSubmissionSet();
                    SubmitObjectsRequest updateRequest = iti67RequestUpdateConverter.createMetadataUpdateRequest(submissionSet, documentEntry);
                    exchange.getMessage().setBody(updateRequest);
                    log.info("Prepared document metadata update request");
                })
                .to(metadataUpdateEndpoint)
                .process(translateToFhir(new Iti67FromIti57ResponseConverter(config), Response.class));

    }
}
