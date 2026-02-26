package ch.bfh.ti.i4mi.mag.common;

import static org.openehealth.ipf.platform.camel.ihe.ws.HeaderUtils.addOutgoingSoapHeaders;
import static org.opensaml.saml.common.xml.SAMLConstants.SAML20_NS;

import java.io.StringReader;
import java.util.Base64;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.headers.Header;
import org.apache.cxf.staxutils.StaxUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;

/**
 * This processor forwards the HTTP request headers (from FHIR) to the SOAP request headers. This is needed for
 * Authorization and traceparent headers.
 *
 * @author Quentin Ligier
 **/
public class RequestHeadersForwarder {
    private static final Logger log = LoggerFactory.getLogger(RequestHeadersForwarder.class);
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String OASIS_WSSECURITY_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

    public static Processor forward() {
        return exchange -> {
            // Forward the Authorization header if present
            forwardAuthToken(exchange);

            // Extract the traceparent header if present and update it for the next hop
            TraceparentHandler.saveHeader(exchange);
            TraceparentHandler.updateHeaderForSoap().process(exchange);
        };
    }

    public static Processor checkAuthorization(final boolean check) {
        return exchange -> {
            final var authorizationHeader = FhirExchanges.readRequestHttpHeader(AUTHORIZATION_HEADER, exchange, false);
            if (check && authorizationHeader == null) {
                throw new AuthenticationException("The Authorization header is missing");
            }
            // TODO verify if the token is valid
        };
    }

    public static void setWsseHeader(final Exchange exchange,
                                     final String assertion) throws XMLStreamException {
        String ns = "";
        if (assertion.startsWith("<saml:Assertion")) {
            // https://github.com/ahdis/MobileAccessGateway/issues/24
            // emedo does not use namespaces prefix definition in the inner assertion (om.ctc.wstx.exc.WstxParsingException: Undeclared namespace prefix "xsi" (for attribute "type"))
            ns = "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:s=\"http://www.w3.org/2001/XMLSchema\" ";
        }
        final var securityXml = String.format("<wsse:Security %sxmlns:wsse=\"%s\">%s</wsse:Security>",
                                              ns, OASIS_WSSECURITY_NS,
                                              assertion);

        final Element security = StaxUtils.read(new StringReader(securityXml)).getDocumentElement();

        final String alias = getAttrValue(security, SAML20_NS, "NameID", "SPProvidedID");
        final String user = getNodeValue(security, SAML20_NS, "NameID");
        final String issuer = getNodeValue(security, SAML20_NS, "Issuer");
        final String userName = alias + "<" + user + "@" + issuer + ">";
        exchange.setProperty("UserName", userName);

        final var newHeader = new SoapHeader(new QName(OASIS_WSSECURITY_NS, "Security"), security);
        newHeader.setDirection(Header.Direction.DIRECTION_OUT);

        addOutgoingSoapHeaders(exchange, newHeader);
    }

    /**
     * Forwards the Authorization header to the next hop.
     * <p>
     * If the Authorization header contains a base64-encoded SAML assertion, it is decoded and a WS-Security header is
     * created from it.
     * </p>
     * <p>
     * If the Authorization header is a JWT or something else, it is forwarded as is.
     * </p>
     */
    private static void forwardAuthToken(final Exchange exchange) throws XMLStreamException {
        final var authorizationHeader = FhirExchanges.readRequestHttpHeader(AUTHORIZATION_HEADER, exchange, true);
        if (authorizationHeader == null) {
            return;
        }

        // Extract the payload from the Authorization header
        final String payload;
        if (authorizationHeader.startsWith("Bearer ")) {
            payload = authorizationHeader.substring("Bearer ".length());
        } else if (authorizationHeader.startsWith("IHE-SAML ")) {
            payload = authorizationHeader.substring("IHE-SAML ".length());
        } else {
            throw new AuthenticationException("The Authorization header is not in a supported format (invalid scheme)");
        }

        if (payload.startsWith("PHNhbWwyOkFzc2") || payload.startsWith("PD94bW") || payload.startsWith("PHNhbWw6QXNzZXJ0aW9u")) {
            // It is an encoded SAML assertion, convert it to a WS-Security header
            log.debug("Converting encoded SAML assertion to WS-Security header");

            String converted = new String(Base64.getDecoder().decode(payload));
            if (converted.startsWith("<?xml")) {
                converted = converted.substring(converted.indexOf(">") + 1);
            }

            setWsseHeader(exchange, converted);
        } else {
            throw new AuthenticationException("The Authorization header is not in a supported format (invalid parameters)");
        }
    }

    private static String getNodeValue(final Element in,
                                       final String ns,
                                       final String element) {
        final NodeList lst = in.getElementsByTagNameNS(ns, element);
        if (lst.getLength() == 0) {
            return "";
        }
        return lst.item(0).getTextContent();

    }

    private static String getAttrValue(final Element in,
                                       final String ns,
                                       final String element,
                                       final String attribute) {
        final NodeList lst = in.getElementsByTagNameNS(ns, element);
        if (lst.getLength() == 0) {
            return "";
        }
        final Node attr = lst.item(0).getAttributes().getNamedItem(attribute);
        return attr != null ? attr.getTextContent() : "";
    }
}
