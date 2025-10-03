package ch.bfh.ti.i4mi.mag.auth;

import ch.bfh.ti.i4mi.mag.common.XmlUtils;
import ch.bfh.ti.i4mi.mag.config.props.MagAuthProps;
import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.impl.ResponseMarshaller;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.Signer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@ConditionalOnProperty({
        "mag.auth.tcu.principal-name",
        "mag.auth.tcu.principal-gln",
        "mag.auth.tcu.keystore-path",
        "mag.auth.tcu.keystore-password",
        "mag.auth.tcu.keystore-alias",
        "mag.auth.tcu.oid"
})
public class TcuAssertionGenerator {

    private final MagAuthProps.MagAuthTcuProps tcuProps;

    public TcuAssertionGenerator(final MagProps magProps) {
        this.tcuProps = magProps.getAuth().getTcu();
    }

    public String generateNew() throws Exception {
        // 1. Get and parse the template file
        final String templateContent = this.getTemplateContent().replace("{TCU_OID}", this.tcuProps.getOid());

        final Element templateElement =
                XmlUtils.newSafeDocumentBuilder().parse(new ByteArrayInputStream(templateContent.getBytes())).getDocumentElement();
        final Assertion assertion = (Assertion) XMLObjectProviderRegistrySupport.getUnmarshallerFactory().getUnmarshaller(templateElement).unmarshall(templateElement);
        this.updateAssertionContent(assertion);

        // 2. Build and configure the Signature object
        final Signature signature = (Signature) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(Signature.DEFAULT_ELEMENT_NAME)
                .buildObject(Signature.DEFAULT_ELEMENT_NAME);

        final Credential signingCredential = this.getSigningCredential();

        signature.setSigningCredential(signingCredential);
        signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

        final var keyInfoGenFactory = new X509KeyInfoGeneratorFactory();
        keyInfoGenFactory.setEmitEntityCertificate(true);
        keyInfoGenFactory.setEmitEntityCertificateChain(false);
        keyInfoGenFactory.setEmitKeyNames(true);
        signature.setKeyInfo(keyInfoGenFactory.newInstance().generate(signingCredential));

        // 4. Attach the signature to the assertion
        assertion.setSignature(signature);

        // 5. Marshall and sign
        XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(assertion).marshall(assertion);
        Signer.signObject(signature);

        final ResponseMarshaller marshaller = new ResponseMarshaller();
        final Element assertionElement = marshaller.marshall(assertion);

        return XmlUtils.serialize(assertionElement);
    }

    private void updateAssertionContent(final Assertion assertion) {
        assertion.setID("Assertion_" + UUID.randomUUID());

        final var issueInstant = Instant.now();
        final var notAfter = issueInstant.plus(10, ChronoUnit.MINUTES);
        assertion.setIssueInstant(issueInstant);

        final Conditions conditions = assertion.getConditions();
        conditions.setNotBefore(issueInstant);
        conditions.setNotOnOrAfter(notAfter);

        AuthnStatement authn = assertion.getAuthnStatements().getFirst();
        authn.setAuthnInstant(issueInstant);
        authn.setSessionNotOnOrAfter(notAfter);
    }

    private String getTemplateContent() throws Exception {
        return new DefaultResourceLoader()
                .getResource("classpath:tcu_assertion_template.xml")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private Credential getSigningCredential() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        final DefaultResourceLoader loader = new DefaultResourceLoader();
        final var is = loader.getResource(this.tcuProps.getKeystorePath()).getInputStream();

        // Load KeyStore
        ks.load(is, this.tcuProps.getKeystorePassword().toCharArray());

        // Get Private Key Entry From Certificate
        final KeyStore.PrivateKeyEntry pkEntry =
                (KeyStore.PrivateKeyEntry) ks.getEntry(
                        this.tcuProps.getKeystoreAlias(),
                        new KeyStore.PasswordProtection(this.tcuProps.getKeystorePassword().toCharArray())
                );

        final PrivateKey pk = pkEntry.getPrivateKey();

        final X509Certificate certificate = (X509Certificate) pkEntry.getCertificate();
        final BasicX509Credential credential = new BasicX509Credential(certificate);
        credential.setEntityCertificate(certificate);
        credential.setPrivateKey(pk);
        return credential;
    }
}
