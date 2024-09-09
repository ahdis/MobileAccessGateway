package ch.bfh.ti.i4mi.mag.mhd;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.MobileAccessGateway;
import ch.bfh.ti.i4mi.mag.xua.AuthTokenConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Dmytro Rud
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import(MobileAccessGateway.class)
@ActiveProfiles("test")
@Slf4j
public class MhdTest {

    public static final String LOGICAL_ID_FOR_DELETE        = "urn:uuid:" + UUID.randomUUID();
    public static final String LOGICAL_ID_FOR_CC_1_LOCAL    = "urn:uuid:" + UUID.randomUUID();
    public static final String LOGICAL_ID_FOR_CC_1_FOREIGN  = "urn:uuid:" + UUID.randomUUID();
    public static final String LOGICAL_ID_FOR_CC_3_LOCAL    = "urn:uuid:" + UUID.randomUUID();
    public static final String LOGICAL_ID_FOR_CC_4_LOCAL    = "urn:uuid:" + UUID.randomUUID();
    public static final String LOGICAL_ID_FOR_CC_5_FOREIGN  = "urn:uuid:" + UUID.randomUUID();

    public static final String HOME_COMMUNITY_ID_LOCAL      = "1.2.3.4.5";
    public static final String HOME_COMMUNITY_ID_FOREIGN    = "3.14.15.926";

    public static final String DOCUMENT_UNIQUE_ID_LOCAL     = HOME_COMMUNITY_ID_LOCAL + ".1";
    public static final String DOCUMENT_UNIQUE_ID_FOREIGN   = HOME_COMMUNITY_ID_FOREIGN + ".1";


    @Autowired
    protected CamelContext camelContext;

    @Autowired
    protected ProducerTemplate producerTemplate;

    @Value("${server.port}")
    protected Integer serverPort;

    private static String httpAuthHeader;
    @Autowired
    private Config config;

    @BeforeAll
    public static void beforeAll() throws Exception {
        Locale.setDefault(Locale.ENGLISH);
        InputStream inputStream = MhdTest.class.getClassLoader().getResourceAsStream("xua-token.base64");
        String base64 = IOUtils.toString(inputStream, StandardCharsets.US_ASCII).replaceAll("\\s", "");
        httpAuthHeader = "Bearer " + base64;

//        System.setProperty("javax.net.ssl.keyStore", "src/main/resources/example-server-certificate.p12");
//        System.setProperty("javax.net.ssl.keyStorePassword", "a1b2c3");
//        System.setProperty("javax.net.ssl.trustStore", "src/main/resources/example-server-certificate.p12");
//        System.setProperty("javax.net.ssl.trustStorePassword", "a1b2c3");
    }

    @Test
    public void testMetadataUpdate() {
        DocumentReference documentReference = (DocumentReference) FhirContext.forR4().newJsonParser().parseResource(MhdTest.class.getClassLoader().getResourceAsStream("update-request-1.json"));
        IGenericClient client = FhirContext.forR4().newRestfulGenericClient("http://localhost:" + serverPort + "/fhir");
        MethodOutcome methodOutcome;

        // test success case
        documentReference.getContent().get(0).getAttachment().setSize(100);
        methodOutcome = client.update()
                .resource(documentReference)
                .withAdditionalHeader(AuthTokenConverter.AUTHORIZATION_HEADER, httpAuthHeader)
                .execute();
        assertEquals(200, methodOutcome.getResponseStatusCode());

        // test failure case
        boolean errorCatched = false;
        try {
            documentReference.getContent().get(0).getAttachment().setSize(101);
            methodOutcome = client.update()
                    .resource(documentReference)
                    .withAdditionalHeader(AuthTokenConverter.AUTHORIZATION_HEADER, httpAuthHeader)
                    .execute();
        } catch (BaseServerResponseException e) {
            assertEquals(400, e.getStatusCode());
            errorCatched = true;
        }
        assertTrue(errorCatched);
    }

    @Test
    public void testDocumentDeletion() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            ClassicHttpRequest httpRequest = ClassicRequestBuilder.delete("http://localhost:" + serverPort + "/fhir/DocumentReference/" + LOGICAL_ID_FOR_DELETE)
                    .setCharset(StandardCharsets.UTF_8)
                    .addHeader(AuthTokenConverter.AUTHORIZATION_HEADER, httpAuthHeader)
                    .build();
            String response = httpClient.execute(httpRequest, httpResponse -> {
                assertEquals(200, httpResponse.getCode());
                return httpResponse.toString();
            });
            log.info(response);
        }
    }

    /* ==================================================================================================
     * Test cases for ITI-18:
     *      1 = multicast to Document Registry and Initiating Gateway, happy case -- FindDocuments with patient ID "testCC-1"
     *      2 = multicast to Document Registry and Initiating Gateway, error in Document Registry -- FindDocuments with patient ID "testCC-2"
     *      3 = multicast to Document Registry and Initiating Gateway, error in Initiating Gateway -- FindDocuments with patient ID "testCC-3"
     *      4 = request forwarded only to Document Registry -- GetDocuments with logical ID == LOGICAL_ID_FOR_CC_4
     *      5 = request forwarded only to Initiating Gateway -- GetDocuments with logical ID == LOGICAL_ID_FOR_CC_5
     * ================================================================================================== */

    @Test
    public void testIti18Dispatching1() throws Exception {
        ICriterion<?> searchParameter = new TokenClientParam("patient.identifier").exactly()
                .systemAndIdentifier("urn:oid:2.16.840.1.113883.3.37.4.1.1.2.1.1", "testCC-1");
        IGenericClient client = FhirContext.forR4().newRestfulGenericClient("http://localhost:" + serverPort + "/fhir");
        Bundle bundle = client.search()
                .forResource(DocumentReference.class)
                .where(searchParameter)
                .withAdditionalHeader(AuthTokenConverter.AUTHORIZATION_HEADER, httpAuthHeader)
                .returnBundle(Bundle.class)
                .execute();
        assertEquals(2, bundle.getEntry().size());

        {
            DocumentReference ref = (DocumentReference) bundle.getEntry().get(0).getResource();
            String id = ref.getId().substring(ref.getId().lastIndexOf('/') + 1);
            assertEquals(LOGICAL_ID_FOR_CC_1_LOCAL.substring("urn:uuid:".length()), id);
        }
        {
            DocumentReference ref = (DocumentReference) bundle.getEntry().get(1).getResource();
            String id = ref.getId().substring(ref.getId().lastIndexOf('/') + 1);
            assertEquals(LOGICAL_ID_FOR_CC_1_FOREIGN.substring("urn:uuid:".length()), id);
        }
    }

    @Test
    public void testIti18Dispatching2() throws Exception {
        ICriterion<?> searchParameter = new TokenClientParam("patient.identifier").exactly()
                .systemAndIdentifier("urn:oid:2.16.840.1.113883.3.37.4.1.1.2.1.1", "testCC-2");
        IGenericClient client = FhirContext.forR4().newRestfulGenericClient("http://localhost:" + serverPort + "/fhir");

        boolean exceptionHandled = false;
        try {
            Bundle bundle = client.search()
                    .forResource(DocumentReference.class)
                    .where(searchParameter)
                    .withAdditionalHeader(AuthTokenConverter.AUTHORIZATION_HEADER, httpAuthHeader)
                    .returnBundle(Bundle.class)
                    .execute();
        } catch (InvalidRequestException e) {
            assertTrue(e.getResponseBody().contains("ITI-18 request to the Document Registry failed"));
            exceptionHandled = true;
        }

        assertTrue(exceptionHandled);
    }

    @Test
    public void testIti18Dispatching3() throws Exception {
        ICriterion<?> searchParameter = new TokenClientParam("patient.identifier").exactly()
                .systemAndIdentifier("urn:oid:2.16.840.1.113883.3.37.4.1.1.2.1.1", "testCC-3");
        IGenericClient client = FhirContext.forR4().newRestfulGenericClient("http://localhost:" + serverPort + "/fhir");

        Bundle bundle = client.search()
                .forResource(DocumentReference.class)
                .where(searchParameter)
                .withAdditionalHeader(AuthTokenConverter.AUTHORIZATION_HEADER, httpAuthHeader)
                .returnBundle(Bundle.class)
                .execute();
        assertEquals(2, bundle.getEntry().size());

        {
            OperationOutcome outcome = (OperationOutcome) bundle.getEntry().get(0).getResource();
            assertEquals(1, outcome.getIssue().size());
            assertEquals("ITI-18 request to the Initiating Gateway failed", outcome.getIssue().get(0).getDetails().getText());
        }
        {
            DocumentReference ref = (DocumentReference) bundle.getEntry().get(1).getResource();
            String id = ref.getId().substring(ref.getId().lastIndexOf('/') + 1);
            assertEquals(LOGICAL_ID_FOR_CC_3_LOCAL.substring("urn:uuid:".length()), id);
        }
    }

    @Test
    public void testIti18Dispatching4() throws Exception {
        ICriterion<?> dummyPatientIdParameter = new TokenClientParam("patient.identifier").exactly()
                .systemAndIdentifier("dummy", "dummy");
        ICriterion<?> logicalIdParameter = new TokenClientParam("identifier").exactly()
                .identifier("urn:uuid:" + LOGICAL_ID_FOR_CC_4_LOCAL);

        IGenericClient client = FhirContext.forR4().newRestfulGenericClient("http://localhost:" + serverPort + "/fhir");

        Bundle bundle = client.search()
                .forResource(DocumentReference.class)
                .where(dummyPatientIdParameter)
                .where(logicalIdParameter)
                .withAdditionalHeader(AuthTokenConverter.AUTHORIZATION_HEADER, httpAuthHeader)
                .returnBundle(Bundle.class)
                .execute();
        assertEquals(1, bundle.getEntry().size());

        {
            DocumentReference ref = (DocumentReference) bundle.getEntry().get(0).getResource();
            assertTrue(ref.getSubject().getReference().endsWith(HOME_COMMUNITY_ID_LOCAL + "-testCC-4"));
        }
    }

    @Test
    public void testIti18Dispatching5() throws Exception {
        ICriterion<?> dummyPatientIdParameter = new TokenClientParam("patient.identifier").exactly()
                .systemAndIdentifier("dummy", "dummy");
        ICriterion<?> logicalIdParameter = new TokenClientParam("identifier").exactly()
                .identifier("urn:uuid:" + LOGICAL_ID_FOR_CC_5_FOREIGN);
        ICriterion<?> homeParameter = new TokenClientParam("home").exactly()
                .identifier("urn:oid:3.14.15.926");

        IGenericClient client = FhirContext.forR4().newRestfulGenericClient("http://localhost:" + serverPort + "/fhir");

        Bundle bundle = client.search()
                .forResource(DocumentReference.class)
                .where(dummyPatientIdParameter)
                .where(logicalIdParameter)
                .where(homeParameter)
                .withAdditionalHeader(AuthTokenConverter.AUTHORIZATION_HEADER, httpAuthHeader)
                .returnBundle(Bundle.class)
                .execute();
        assertEquals(1, bundle.getEntry().size());

        {
            DocumentReference ref = (DocumentReference) bundle.getEntry().get(0).getResource();
            assertTrue(ref.getSubject().getReference().endsWith(HOME_COMMUNITY_ID_FOREIGN + "-testCC-5"));
        }
    }

    @Test
    public void testIti68Local1() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            ClassicHttpRequest httpRequest = ClassicRequestBuilder.get("http://localhost:9090/camel/xdsretrieve?repositoryUniqueId=1.2.3&uniqueId=" + DOCUMENT_UNIQUE_ID_LOCAL)
                    .setCharset(StandardCharsets.UTF_8)
                    .addHeader(AuthTokenConverter.AUTHORIZATION_HEADER, httpAuthHeader)
                    .build();
            String response = httpClient.execute(httpRequest, httpResponse -> {
                assertEquals(200, httpResponse.getCode());
                return new String(httpResponse.getEntity().getContent().readAllBytes());
            });
            assertEquals("<tag/>", response);
        }
    }

    @Test
    public void testIti68Local2() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            ClassicHttpRequest httpRequest = ClassicRequestBuilder.get("http://localhost:9090/camel/xdsretrieve?homeCommunityId=" + config.getHomeCommunity() + "&repositoryUniqueId=1.2.3&uniqueId=" + DOCUMENT_UNIQUE_ID_LOCAL)
                    .setCharset(StandardCharsets.UTF_8)
                    .addHeader(AuthTokenConverter.AUTHORIZATION_HEADER, httpAuthHeader)
                    .build();
            String response = httpClient.execute(httpRequest, httpResponse -> {
                assertEquals(200, httpResponse.getCode());
                return new String(httpResponse.getEntity().getContent().readAllBytes());
            });
            assertEquals("<tag/>", response);
        }
    }

    @Test
    public void testIti68Foreign() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            ClassicHttpRequest httpRequest = ClassicRequestBuilder.get("http://localhost:9090/camel/xdsretrieve?homeCommunityId=3.354.23.12&repositoryUniqueId=1.2.3&uniqueId=" + DOCUMENT_UNIQUE_ID_FOREIGN)
                    .setCharset(StandardCharsets.UTF_8)
                    .addHeader(AuthTokenConverter.AUTHORIZATION_HEADER, httpAuthHeader)
                    .build();
            String response = httpClient.execute(httpRequest, httpResponse -> {
                assertEquals(200, httpResponse.getCode());
                return new String(httpResponse.getEntity().getContent().readAllBytes());
            });
            assertEquals("{ }", response);
        }
    }

}
