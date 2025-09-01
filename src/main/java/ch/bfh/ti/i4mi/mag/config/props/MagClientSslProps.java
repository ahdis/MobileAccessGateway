package ch.bfh.ti.i4mi.mag.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mag.client-ssl")
public class MagClientSslProps {

    private boolean enabled;
    private StoreProps keyStore;
    private StoreProps truststore;
    private String certAlias;

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public StoreProps getKeyStore() {
        return this.keyStore;
    }

    public void setKeyStore(final StoreProps keyStore) {
        this.keyStore = keyStore;
    }

    public StoreProps getTruststore() {
        return this.truststore;
    }

    public void setTruststore(final StoreProps truststore) {
        this.truststore = truststore;
    }

    public String getCertAlias() {
        return this.certAlias;
    }

    public void setCertAlias(final String certAlias) {
        this.certAlias = certAlias;
    }
}
