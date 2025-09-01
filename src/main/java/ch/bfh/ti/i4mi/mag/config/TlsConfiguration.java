package ch.bfh.ti.i4mi.mag.config;

import ch.bfh.ti.i4mi.mag.config.props.MagClientSslProps;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.openehealth.ipf.boot.atna.IpfAtnaConfigurationProperties;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.DefaultAuditContext;
import org.openehealth.ipf.commons.audit.protocol.TCPSyslogSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyStore;
import java.security.KeyStoreException;

@Configuration
public class TlsConfiguration {

    public static final String BEAN_TLS_CONTEXT_WS = "wsTlsContext";
    public static final String BEAN_TLS_CONTEXT_ATNA = "auditTlsContext";

    @Bean(name = BEAN_TLS_CONTEXT_WS)
    @ConditionalOnProperty(
            value = "mag.client-ssl.enabled",
            havingValue = "true",
            matchIfMissing = false)
    public SSLContextParameters getWsTlsContext(final MagClientSslProps magProps) throws KeyStoreException {
        final var ksp = new KeyStoreParameters();

        // https://www.baeldung.com/java-keystore
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        // Keystore file may be found at src/main/resources
        ksp.setResource(magProps.getKeyStore().getPath());
        ksp.setPassword(magProps.getKeyStore().getPassword());

        final var kmp = new KeyManagersParameters();
        kmp.setKeyStore(ksp);
        kmp.setKeyPassword(magProps.getKeyStore().getPassword());

        final var tsp = new KeyStoreParameters();
        // Truststore file may be found at src/main/resources
        tsp.setResource(magProps.getTruststore().getPath());
        tsp.setPassword(magProps.getTruststore().getPassword());

        final var tmp = new TrustManagersParameters();
        tmp.setKeyStore(tsp);

        final var scp = new SSLContextParameters();
        scp.setKeyManagers(kmp);
        scp.setTrustManagers(tmp);
        //scp.setCertAlias(certAlias);

        return scp;
    }

    @Bean(name = BEAN_TLS_CONTEXT_ATNA)
    public AuditContext getAuditContext(final IpfAtnaConfigurationProperties auditProps,
                                        final MagClientSslProps magProps) {
        final var context = new DefaultAuditContext();
        if (auditProps.isAuditEnabled()) {
            final var ksp = new KeyStoreParameters();

            // https://www.baeldung.com/java-keystore
            // Keystore file may be found at src/main/resources
            ksp.setResource(magProps.getKeyStore().getPath());
            ksp.setPassword(magProps.getKeyStore().getPassword());

            final var kmp = new KeyManagersParameters();
            kmp.setKeyStore(ksp);
            kmp.setKeyPassword(magProps.getKeyStore().getPassword());

            final var tmp = new TrustManagersParameters();
            tmp.setKeyStore(ksp);

            final var scp = new SSLContextParameters();
            scp.setKeyManagers(kmp);
            scp.setTrustManagers(tmp);
            scp.setCertAlias(magProps.getCertAlias());
            context.setTlsParameters(b -> {
                try {
                    return scp.createSSLContext(scp.getCamelContext());
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        context.setAuditTransmissionProtocol(new TCPSyslogSender());

        return context;
    }
}
