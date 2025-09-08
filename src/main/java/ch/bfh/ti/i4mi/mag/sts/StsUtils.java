package ch.bfh.ti.i4mi.mag.sts;

import ch.bfh.ti.i4mi.mag.common.XmlUtils;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import net.shibboleth.shared.xml.XMLParserException;
import net.shibboleth.shared.xml.impl.BasicParserPool;
import org.apache.camel.Body;
import org.apache.camel.ExchangeProperty;
import org.apache.camel.Header;
import org.apache.cxf.staxutils.StaxUtils;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.opensaml.saml.common.xml.SAMLConstants.SAML20_NS;

@Component
public class StsUtils {

    public static final String SCOPE_PURPOSEOFUSE = "purpose_of_use=";
    public static final String RESOURCE_ID = "person_id=";
    public static final String ROLE = "subject_role=";
    public static final String PRINCIPAL_ID = "principal_id=";
    public static final String PRINCIPAL_NAME = "principal=";
    public static final String ORGANIZATION_ID = "group_id=";
    public static final String ORGANIZATION_NAME = "group=";

    /**
     * The unmarshaller of Assertion elements.
     */
    private final Unmarshaller assertionUnmarshaller;

    /**
     * The SAML parser pool.
     */
    private final BasicParserPool samlParserPool;

    public StsUtils() {
        this.assertionUnmarshaller = XMLObjectProviderRegistrySupport
                .getUnmarshallerFactory()
                .getUnmarshaller(Assertion.DEFAULT_ELEMENT_NAME);
        this.samlParserPool = new BasicParserPool();
        this.samlParserPool.setNamespaceAware(true);
    }

    public OAuth2TokenResponse generateOAuth2TokenResponse(final @ExchangeProperty("oauthrequest") AuthenticationRequest authRequest,
                                                           final @Body String assertion,
                                                           final @Header("scope") String scope)
            throws XMLParserException, UnmarshallingException {

        final String encoded = Base64.getEncoder().encodeToString(assertion.getBytes(StandardCharsets.UTF_8));
        final String idpAssertion = authRequest.getIdpAssertion();
        final String encodedIdp = Base64.getEncoder().encodeToString(idpAssertion.getBytes(StandardCharsets.UTF_8));

        final var result = new OAuth2TokenResponse();
        result.setAccess_token(encoded);
        result.setRefresh_token(encodedIdp);
        result.setExpires_in(this.computeExpiresInFromNotOnOrAfter(assertion)); // In seconds
        result.setScope(scope);
        result.setToken_type("Bearer" /*request.getToken_type()*/);
        return result;
    }

    /**
     * Computes the number of seconds from now (inclusive) to the Assertion's @NotOnOrAfter attribute (exclusive).
     *
     * @param assertionXml The XML representation of the Assertion.
     * @return a duration in seconds.
     * @throws XMLParserException     if the Assertion cannot be parsed.
     * @throws UnmarshallingException if the Assertion cannot be unmarshalled.
     */
    private long computeExpiresInFromNotOnOrAfter(final String assertionXml)
            throws XMLParserException, UnmarshallingException {
        // Parse the assertion and extract the NotOnOrAfter attribute
        final Element element = this.samlParserPool
                .parse(new ByteArrayInputStream(assertionXml.getBytes(StandardCharsets.UTF_8)))
                .getDocumentElement();
        final Assertion assertion = (Assertion) this.assertionUnmarshaller.unmarshall(element);

        final Instant notOnOrAfter = assertion.getConditions().getNotOnOrAfter();
        return Duration.between(Instant.now(), notOnOrAfter).getSeconds();
    }

    public ErrorResponse handleError(final @Body AuthException in) {
        final var response = new ErrorResponse();
        response.setError(in.getError());
        response.setError_description(in.getMessage());
        return response;
    }

    public AuthenticationRequest emptyAuthRequest() {
        return new AuthenticationRequest();
    }

    public AssertionRequest keepIdpAssertion(final @ExchangeProperty("oauthrequest") AuthenticationRequest authRequest,
                                             final @Body AssertionRequest assertionRequest) throws Exception {
        final String idpAssertion;
        if (assertionRequest.getSamlToken() instanceof String) {
            idpAssertion = (String) assertionRequest.getSamlToken();
        } else {
            idpAssertion = XmlUtils.serialize((Node) assertionRequest.getSamlToken());
        }
        authRequest.setIdpAssertion(idpAssertion);
        return assertionRequest;
    }

    public AssertionRequest buildAssertionRequestFromIdp(final @Body String authorization,
                                                         final @Header("scope") String scope) throws AuthException {
        return this.buildAssertionRequestInternal(authorization, scope);
    }

    private AssertionRequest buildAssertionRequestInternal(final Object authorization,
                                                           final String scope) throws AuthException {
        if (authorization == null) {
            throw this.throwInvalidRequest("missing IDP token");
        }
        if (scope == null || scope.isEmpty()) {
            throw this.throwInvalidRequest("missing scope parameter");
        }
        final var result = new AssertionRequest();
        for (final String scopePart : scope.split("\\s")) {
            if (scopePart.startsWith(SCOPE_PURPOSEOFUSE)) {
                result.setPurposeOfUse(token(scopePart.substring(SCOPE_PURPOSEOFUSE.length()),
                                             "urn:oid:2.16.756.5.30.1.127.3.10.5"));
            }
            if (scopePart.startsWith(RESOURCE_ID)) {
                result.setResourceId(scopePart.substring(RESOURCE_ID.length()));
            }
            if (scopePart.startsWith(ROLE)) {
                result.setRole(token(scopePart.substring(ROLE.length()), "urn:oid:2.16.756.5.30.1.127.3.10.6"));
            }
            if (scopePart.startsWith(PRINCIPAL_ID)) {
                result.setPrincipalID(scopePart.substring(PRINCIPAL_ID.length()));
            }
            if (scopePart.startsWith(PRINCIPAL_NAME)) {
                result.setPrincipalName(decode(scopePart.substring(PRINCIPAL_NAME.length())));
            }
            if (scopePart.startsWith(ORGANIZATION_ID)) {
                result.addOrganizationID(scopePart.substring(ORGANIZATION_ID.length()));
            }
            if (scopePart.startsWith(ORGANIZATION_NAME)) {
                result.addOrganizationName(decode(scopePart.substring(ORGANIZATION_NAME.length())));
            }
        }
        result.setSamlToken(authorization);
        return result;
    }

    private AuthException throwInvalidRequest(final String message) {
        return new AuthException(400, "invalid_request", message);
    }

    private String token(final String token, final String system) throws AuthException {
        if (token.startsWith(system + "|")) {
            return token.substring(system.length() + 1);
        }
        if (token.startsWith("|")) {
            return token.substring(1);
        }
        if (!token.contains("|")) {
            return token;
        }
        throw this.throwInvalidRequest("Invalid scope");
    }

    private String decode(String in) {
        return java.net.URLDecoder.decode(in, StandardCharsets.UTF_8);
    }

    public String extractAssertionAsString(final @Body SOAPMessage in) throws SOAPException {
        SOAPBody body = in.getSOAPBody();
        NodeList lst = body.getElementsByTagNameNS(SAML20_NS, "Assertion");
        Node node = lst.item(0);
        // TODO: omit xml declaration?
        return StaxUtils.toString(node);
    }
}
