package ch.bfh.ti.i4mi.mag.auth;

import ch.bfh.ti.i4mi.mag.config.props.MagAuthProps;
import jakarta.xml.soap.*;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.apache.cxf.staxutils.StaxUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.StringReader;
import java.util.UUID;

import static org.opensaml.saml.common.xml.SAMLConstants.SAML20_NS;

@Service
public class StsService implements CamelContextAware {
    private static final Logger log = LoggerFactory.getLogger(StsService.class);

    private static final String WSA = "http://www.w3.org/2005/08/addressing";
    private static final String WST = "http://docs.oasis-open.org/ws-sx/ws-trust/200512";
    private static final String HL7V3 = "urn:hl7-org:v3";
    private static final String WSSE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

    private final MagAuthProps authProps;
    private CamelContext camelContext;
    private ProducerTemplate producerTemplate;

    public StsService(final MagAuthProps authProps) {
        this.authProps = authProps;
    }

    @Override
    public CamelContext getCamelContext() {
        return this.camelContext;
    }

    @Override
    public void setCamelContext(final CamelContext camelContext) {
        this.camelContext = camelContext;
        this.producerTemplate = camelContext.createProducerTemplate();
    }

    public String requestXua(final String eprSpid,
                             final PurposeOfUse purposeOfUse,
                             final Role role,
                             final String principalName,
                             final String principalGln,
                             final String idpAssertion) throws Exception {
        final SOAPMessage soapMessage = this.initSoapMessage();
        soapMessage.getSOAPHeader().addChildElement("MessageID", "wsa", WSA).addTextNode(UUID.randomUUID().toString());
        soapMessage.getSOAPHeader().addChildElement("To", "wsa", WSA).addTextNode(this.authProps.getSts());
        {
            final Element elem = StaxUtils.read(new StringReader(idpAssertion)).getDocumentElement();
            final Node node = soapMessage.getSOAPHeader().getOwnerDocument().importNode(elem, true);
            soapMessage.getSOAPHeader().addChildElement("Security", "wsse", WSSE).appendChild(node);
        }

        soapMessage.getSOAPBody().getElementsByTagNameNS(WSA, "Address").item(0)
                .setTextContent(this.authProps.getStsIssuer());

        final var claims = soapMessage.getSOAPBody().getElementsByTagNameNS(WST, "Claims").item(0);
        this.addEprSpid(claims, eprSpid);
        this.addPurposeOfUse(claims, purposeOfUse);
        this.addRole(claims, role);
        this.addPrincipalGln(claims, principalGln);
        this.addPrincipalName(claims, principalName);

        soapMessage.saveChanges();

        final var exchange = new DefaultExchange(getCamelContext());
        exchange.getIn().setBody(soapMessage);
        final Exchange result = this.producerTemplate.send("direct:sts", exchange);
        if (result.getException() != null) {
            log.warn("Error during STS query", result.getException());
            throw result.getException();
        }
        return result.getMessage().getBody(String.class);
    }

    private SOAPMessage initSoapMessage() throws IOException, SOAPException {
        final var template = new ClassPathResource("iti40_template.xml");
        final MessageFactory factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        return factory.createMessage(new MimeHeaders(), template.getInputStream());
    }

    private void addPurposeOfUse(final Node claims, final PurposeOfUse purposeOfUse) {
        final var doc = claims.getOwnerDocument();
        final var pou = doc.createElementNS(HL7V3, "PurposeOfUse");
        pou.setAttribute("code", purposeOfUse.name());
        pou.setAttribute("codeSystem", "2.16.756.5.30.1.127.3.10.5");
        pou.setAttribute("codeSystemName", "eHealth Suisse Verwendungszweck");
        pou.setAttribute("displayName", purposeOfUse.getDisplay());

        this.addAttribute(claims,
                          "urn:oasis:names:tc:xspa:1.0:subject:purposeofuse",
                          null,
                          null,
                          pou);
    }

    private void addRole(final Node claims, final Role role) {
        final var doc = claims.getOwnerDocument();
        final var rol = doc.createElementNS(HL7V3, "Role");
        rol.setAttribute("code", role.name());
        rol.setAttribute("codeSystem", "2.16.756.5.30.1.127.3.10.6");
        rol.setAttribute("codeSystemName", "eHealth Suisse EPR Akteure");
        rol.setAttribute("displayName", role.getDisplay());

        this.addAttribute(claims,
                          "urn:oasis:names:tc:xacml:2.0:subject:role",
                          "urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified",
                          null,
                          rol);
    }

    private void addEprSpid(final Node claims, final String eprSpid) {
        this.addAttribute(claims,
                          "urn:oasis:names:tc:xacml:2.0:resource:resource-id",
                          null,
                          "%s^^^&2.16.756.5.30.1.127.3.10.3&ISO".formatted(eprSpid),
                          null
        );
    }

    private void addPrincipalName(final Node claims, final String principalName) {
        this.addAttribute(claims,
                          "urn:e-health-suisse:principal-name",
                          null,
                          principalName,
                          null);
    }

    private void addPrincipalGln(final Node claims, final String principalGln) {
        this.addAttribute(claims,
                          "urn:e-health-suisse:principal-id",
                          null,
                          principalGln,
                          null);
    }

    private void addAttribute(final Node claims,
                              final String name,
                              final String nameFormat,
                              final String textContent,
                              final Node nodeContent) {
        final var doc = claims.getOwnerDocument();
        final var attribute = doc.createElementNS(SAML20_NS, "Attribute");
        attribute.setAttribute("Name", name);
        if (nameFormat != null) {
            attribute.setAttribute("NameFormat", nameFormat);
        }

        final var attributeValue = doc.createElementNS(SAML20_NS, "AttributeValue");
        if (textContent != null) {
            attributeValue.setTextContent(textContent);
        }
        if (nodeContent != null) {
            attributeValue.appendChild(nodeContent);
        }
        attribute.appendChild(attributeValue);
        claims.appendChild(attribute);
    }
}
