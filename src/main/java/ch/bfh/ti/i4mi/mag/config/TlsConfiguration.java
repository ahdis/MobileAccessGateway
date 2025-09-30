package ch.bfh.ti.i4mi.mag.config;

import ch.bfh.ti.i4mi.mag.config.props.MagClientSslProps;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.openehealth.ipf.commons.audit.TlsParameters;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TlsConfiguration {

    public static final String BEAN_TLS_CONTEXT_WS = "wsTlsContext";

    @Bean(name = BEAN_TLS_CONTEXT_WS)
    public SSLContextParameters getWsTlsContext(final MagClientSslProps magProps) {

        if (!magProps.isEnabled()) {
            return  new SSLContextParameters();
        }
        final var ksp = new KeyStoreParameters();

        // https://www.baeldung.com/java-keystore
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

    @Bean
    @ConditionalOnProperty(
            value = "mag.client-ssl.enabled",
            havingValue = "true",
            matchIfMissing = false)
    public TlsParameters getAtnaTlsParameters(final MagClientSslProps magProps) {
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
        return b -> {
            try {
                return scp.createSSLContext(scp.getCamelContext());
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
