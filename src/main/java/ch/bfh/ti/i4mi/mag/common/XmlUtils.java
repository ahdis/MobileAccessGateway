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
    public static DocumentBuilder newSafeDocumentBuilder() throws ParserConfigurationException {
        final var factory = DocumentBuilderFactory.newDefaultInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://apache.org/xml/features/xinclude", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        return factory.newDocumentBuilder();
    }

    /**
     * Initializes and configures a {@link Transformer}.
     *
     * @return a configured {@link Transformer}.
     * @throws TransformerConfigurationException if it is not possible to create a {@link Transformer} instance.
     */
    public static Transformer newTransformer() throws TransformerConfigurationException {
        final var transformerFactory = TransformerFactory.newDefaultInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return transformerFactory.newTransformer();
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
