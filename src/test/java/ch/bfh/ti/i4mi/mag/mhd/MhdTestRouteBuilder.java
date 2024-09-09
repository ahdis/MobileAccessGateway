package ch.bfh.ti.i4mi.mag.mhd;

import ch.bfh.ti.i4mi.mag.MagConstants;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.cxf.headers.Header;
import org.glassfish.jersey.message.internal.DataSourceProvider;
import org.openehealth.ipf.commons.ihe.xds.core.SampleData;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Version;
import org.openehealth.ipf.commons.ihe.xds.core.requests.DocumentReference;
import org.openehealth.ipf.commons.ihe.xds.core.requests.QueryRegistry;
import org.openehealth.ipf.commons.ihe.xds.core.requests.RegisterDocumentSet;
import org.openehealth.ipf.commons.ihe.xds.core.requests.RetrieveDocumentSet;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindDocumentsQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.GetDocumentsQuery;
import org.openehealth.ipf.commons.ihe.xds.core.responses.*;
import org.openehealth.ipf.platform.camel.ihe.ws.AbstractWsEndpoint;
import org.openehealth.ipf.platform.camel.ihe.xds.XdsCamelValidators;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dmytro Rud
 */
@Component
public class MhdTestRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        onException(Exception.class)
                .handled(true)
                .maximumRedeliveries(0);

        from("xds-iti18://iti18Endpoint")
                .process(XdsCamelValidators.iti18RequestValidator())
                .process(exchange -> {
                    checkXuaToken(exchange);

                    log.info("Received ITI-18 request in Document Registry");
                    QueryRegistry iti18Request = exchange.getIn().getMandatoryBody(QueryRegistry.class);
                    if (iti18Request.getQuery() instanceof GetDocumentsQuery) {
                        GetDocumentsQuery getDocumentsQuery = (GetDocumentsQuery) iti18Request.getQuery();
                        String logicalId = getDocumentsQuery.getLogicalUuid().get(0);

                        // test HTTP method DELETE
                        if (MhdTest.LOGICAL_ID_FOR_DELETE.equals(logicalId)) {
                            DocumentEntry documentEntry = SampleData.createDocumentEntry(new Identifiable("testIti18-1", new AssigningAuthority(MhdTest.HOME_COMMUNITY_ID_LOCAL)));
                            documentEntry.assignEntryUuid();
                            documentEntry.setVersion(new Version("42"));
                            QueryResponse iti18Response = new QueryResponse(Status.SUCCESS);
                            iti18Response.getDocumentEntries().add(documentEntry);
                            exchange.getMessage().setBody(iti18Response);
                        }
                        // test Cross-Community where only the local Registry shall be queried
                        else if (MhdTest.LOGICAL_ID_FOR_CC_4_LOCAL.equals(logicalId)) {
                            DocumentEntry documentEntry = SampleData.createDocumentEntry(new Identifiable("testCC-4", new AssigningAuthority(MhdTest.HOME_COMMUNITY_ID_LOCAL)));
                            documentEntry.assignEntryUuid();
                            documentEntry.setVersion(new Version("42"));
                            QueryResponse iti18Response = new QueryResponse(Status.SUCCESS);
                            iti18Response.getDocumentEntries().add(documentEntry);
                            exchange.getMessage().setBody(iti18Response);
                        } else {
                            throw new Exception("This statement shall be not reachable");
                        }
                    }

                    if (iti18Request.getQuery() instanceof FindDocumentsQuery) {
                        FindDocumentsQuery findDocumentsQuery = (FindDocumentsQuery) iti18Request.getQuery();
                        DocumentEntry documentEntry = SampleData.createDocumentEntry(findDocumentsQuery.getPatientId());
                        documentEntry.assignEntryUuid();
                        QueryResponse iti18Response = new QueryResponse(Status.SUCCESS);
                        iti18Response.getDocumentEntries().add(documentEntry);
                        exchange.getMessage().setBody(iti18Response);

                        switch (findDocumentsQuery.getPatientId().getId()) {
                            case "testCC-1":
                                // Cross-Community multicast + aggregation, happy case
                                documentEntry.setLogicalUuid(MhdTest.LOGICAL_ID_FOR_CC_1_LOCAL.substring("urn:uuid:".length()));
                                break;
                            case "testCC-2":
                                // Cross-Community multicast + aggregation, error in the Document Registry
                                throw new Exception("ITI-18 request to the Document Registry failed");
                            case "testCC-3":
                                // Cross-Community multicast + aggregation, error in a foreign community
                                documentEntry.setLogicalUuid(MhdTest.LOGICAL_ID_FOR_CC_3_LOCAL.substring("urn:uuid:".length()));
                                break;
                            default:
                                throw new Exception("This statement shall be not reachable");
                        }
                    }
                })
                .process(XdsCamelValidators.iti18ResponseValidator())
        ;

        from("xds-iti18://iti18EndpointInitiatingGateway")
                .process(XdsCamelValidators.iti18RequestValidator())
                .process(exchange -> {
                    checkXuaToken(exchange);

                    log.info("Received ITI-18 request in Initiating Gateway");
                    QueryRegistry iti18Request = exchange.getIn().getMandatoryBody(QueryRegistry.class);

                    if (iti18Request.getQuery() instanceof GetDocumentsQuery) {
                        GetDocumentsQuery getDocumentsQuery = (GetDocumentsQuery) iti18Request.getQuery();
                        // test Cross-Community where only the Initiating Gateway shall be queried
                        if (MhdTest.LOGICAL_ID_FOR_CC_5_FOREIGN.equals(getDocumentsQuery.getLogicalUuid().get(0))) {
                            DocumentEntry documentEntry = SampleData.createDocumentEntry(new Identifiable("testCC-5", new AssigningAuthority(MhdTest.HOME_COMMUNITY_ID_FOREIGN)));
                            documentEntry.assignEntryUuid();
                            documentEntry.setVersion(new Version("42"));
                            QueryResponse iti18Response = new QueryResponse(Status.SUCCESS);
                            iti18Response.getDocumentEntries().add(documentEntry);
                            exchange.getMessage().setBody(iti18Response);
                        } else {
                            throw new Exception("This statement shall be not reachable");
                        }
                    }

                    if (iti18Request.getQuery() instanceof FindDocumentsQuery) {
                        FindDocumentsQuery findDocumentsQuery = (FindDocumentsQuery) iti18Request.getQuery();
                        DocumentEntry documentEntry = SampleData.createDocumentEntry(findDocumentsQuery.getPatientId());
                        documentEntry.assignEntryUuid();
                        QueryResponse iti18Response = new QueryResponse(Status.SUCCESS);
                        iti18Response.getDocumentEntries().add(documentEntry);
                        exchange.getMessage().setBody(iti18Response);

                        switch (findDocumentsQuery.getPatientId().getId()) {
                            case "testCC-1":
                                // Cross-Community multicast + aggregation, happy case
                                documentEntry.setLogicalUuid(MhdTest.LOGICAL_ID_FOR_CC_1_FOREIGN);
                                break;
                            case "testCC-3":
                                throw new Exception("ITI-18 request to the Initiating Gateway failed");
                            default:
                                throw new Exception("This statement shall be not reachable");
                        }
                    }
                })
                .process(XdsCamelValidators.iti18ResponseValidator())
        ;

        from("xds-iti43://iti43Endpoint")
                .process(XdsCamelValidators.iti43RequestValidator())
                .process(exchange -> {
                    checkXuaToken(exchange);

                    log.info("Received ITI-43 request in Document Repository");
                    RetrieveDocumentSet iti43Request = exchange.getIn().getMandatoryBody(RetrieveDocumentSet.class);
                    DocumentReference ref = iti43Request.getDocuments().get(0);
                    if (ref.getHomeCommunityId() != null) {
                        throw new Exception("Did not expect home community ID in ITI-43 request to Document Repository");
                    }
                    if (MhdTest.DOCUMENT_UNIQUE_ID_LOCAL.equalsIgnoreCase(ref.getDocumentUniqueId())) {
                        DataSourceProvider.ByteArrayDataSource dataSource = new DataSourceProvider.ByteArrayDataSource(new ByteArrayInputStream("<tag/>".getBytes()), "text/xml");
                        RetrievedDocument document = new RetrievedDocument(new DataHandler(dataSource), ref, null, null, "text/xml");
                        RetrievedDocumentSet iti43Response = new RetrievedDocumentSet(Status.SUCCESS);
                        iti43Response.getDocuments().add(document);
                        exchange.getMessage().setBody(iti43Response);
                    } else {
                        throw new Exception("This statement shall be not reachable");
                    }
                })
                .process(XdsCamelValidators.iti43ResponseValidator())
        ;

        from("xds-iti43://iti43EndpointInitiatingGateway")
                .process(XdsCamelValidators.iti43RequestValidator())
                .process(exchange -> {
                    checkXuaToken(exchange);

                    log.info("Received ITI-43 request in Initiating Gateway");
                    RetrieveDocumentSet iti43Request = exchange.getIn().getMandatoryBody(RetrieveDocumentSet.class);
                    DocumentReference ref = iti43Request.getDocuments().get(0);
                    if (ref.getHomeCommunityId() == null) {
                        throw new Exception("Home community ID shall be provided in ITI-43 request to Initiating Gateway");
                    }
                    if (MhdTest.DOCUMENT_UNIQUE_ID_FOREIGN.equalsIgnoreCase(ref.getDocumentUniqueId())) {
                        DataSourceProvider.ByteArrayDataSource dataSource = new DataSourceProvider.ByteArrayDataSource(new ByteArrayInputStream("{ }".getBytes()), "text/json");
                        RetrievedDocument document = new RetrievedDocument(new DataHandler(dataSource), ref, null, null, "text/json");
                        RetrievedDocumentSet iti43Response = new RetrievedDocumentSet(Status.SUCCESS);
                        iti43Response.getDocuments().add(document);
                        exchange.getMessage().setBody(iti43Response);
                    } else {
                        throw new Exception("This statement shall be not reachable");
                    }
                })
                .process(XdsCamelValidators.iti43ResponseValidator())
        ;

        from("xds-iti57://iti57Endpoint")
                .process(XdsCamelValidators.iti57RequestValidator())
                .to("direct:handle-metadata-update")
                .process(XdsCamelValidators.iti57ResponseValidator())
        ;

        from("rmu-iti92://iti92Endpoint")
                .process(XdsCamelValidators.iti92RequestValidator())
                .to("direct:handle-metadata-update")
                .process(XdsCamelValidators.iti92ResponseValidator())
        ;

        from("direct:handle-metadata-update")
                .process(exchange -> {
                    checkXuaToken(exchange);

                    log.info("Received metadata update request");
                    RegisterDocumentSet updateRequest = exchange.getIn().getMandatoryBody(RegisterDocumentSet.class);
                    Response updateResponse = new Response();

                    if ("testIti18-1".equals(updateRequest.getSubmissionSet().getPatientId().getId())) {
                        assertEquals(1, updateRequest.getAssociations().size());
                        assertEquals("42", updateRequest.getAssociations().get(0).getPreviousVersion());

                        assertEquals(1, updateRequest.getDocumentEntries().size());
                        DocumentEntry documentEntry = updateRequest.getDocumentEntries().get(0);
                        assertEquals("43", documentEntry.getVersion().getVersionName());
                        assertEquals(1, documentEntry.getExtraMetadata().size());
                        List<String> deletionStatuses = documentEntry.getExtraMetadata().get(MagConstants.XdsExtraMetadataSlotNames.CH_DELETION_STATUS);
                        assertEquals(1, deletionStatuses.size());
                        assertEquals(MagConstants.DeletionStatuses.REQUESTED, deletionStatuses.get(0));

                        updateResponse.setStatus(Status.SUCCESS);
                    } else {
                        RegisterDocumentSet request = exchange.getIn().getMandatoryBody(RegisterDocumentSet.class);
                        updateResponse.setStatus((request.getDocumentEntries().get(0).getSize() % 2 == 0) ? Status.SUCCESS : Status.FAILURE);
                    }
                    exchange.getMessage().setBody(updateResponse);
                })
        ;

    }

    private static void checkXuaToken(Exchange exchange) {
        Map<QName, Header> soapHeaders = exchange.getIn().getHeader(AbstractWsEndpoint.INCOMING_SOAP_HEADERS, Map.class);
        assertTrue(soapHeaders.keySet().stream().anyMatch(name -> "Security".equals(name.getLocalPart())));
    }

}
