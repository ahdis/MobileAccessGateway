package ch.bfh.ti.i4mi.mag.common;

import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;

public class XmlUtils {

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newDefaultInstance();
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newDefaultInstance();

    static {
        try {
            DOCUMENT_BUILDER_FACTORY.setNamespaceAware(true);
            DOCUMENT_BUILDER_FACTORY.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
            DOCUMENT_BUILDER_FACTORY.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DOCUMENT_BUILDER_FACTORY.setFeature("http://apache.org/xml/features/xinclude", false);
            DOCUMENT_BUILDER_FACTORY.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DOCUMENT_BUILDER_FACTORY.setXIncludeAware(false);
            DOCUMENT_BUILDER_FACTORY.setExpandEntityReferences(false);
            DOCUMENT_BUILDER_FACTORY.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DOCUMENT_BUILDER_FACTORY.setFeature("http://xml.org/sax/features/external-general-entities", false);

            TRANSFORMER_FACTORY.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            TRANSFORMER_FACTORY.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This class is not instantiable.
     */
    private XmlUtils() {
    }

    /**
     * Initializes and configures a {@link DocumentBuilder} that is not vulnerable to XXE injections (XInclude, Billions
     * Laugh Attack, …).
     *
     * @return a configured {@link DocumentBuilder}.
     * @throws ParserConfigurationException if the parser is not Xerces2 compatible.
     * @see <a
     * href="https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html#jaxp-documentbuilderfactory-saxparserfactory-and-dom4j">XML
     * External Entity Prevention Cheat Sheet</a>
     */
    public static synchronized DocumentBuilder newSafeDocumentBuilder() throws ParserConfigurationException {
        return DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
    }

    /**
     * Initializes and configures a {@link Transformer}.
     *
     * @return a configured {@link Transformer}.
     * @throws TransformerConfigurationException if it is not possible to create a {@link Transformer} instance.
     */
    public static synchronized Transformer newTransformer() throws TransformerConfigurationException {
        return TRANSFORMER_FACTORY.newTransformer();
    }

    public static String serialize(final Node inputNode) throws Exception {
        final var serializerOutput = new ByteArrayOutputStream();
        final var sourceObject = new DOMSource(inputNode);
        final var targetObject = new StreamResult(serializerOutput);
        final Transformer serializer = newTransformer();
        serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        serializer.transform(sourceObject, targetObject);
        return serializerOutput.toString();
    }
}
