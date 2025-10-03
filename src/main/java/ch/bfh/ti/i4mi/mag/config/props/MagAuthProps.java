package ch.bfh.ti.i4mi.mag.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mag.auth")
public class MagAuthProps {

    private String sts;
    private String stsIssuer;
    private MagAuthTcuProps tcu;

    public String getSts() {
        return this.sts;
    }

    public void setSts(final String sts) {
        this.sts = sts;
    }

    public String getStsIssuer() {
        return this.stsIssuer;
    }

    public void setStsIssuer(final String stsIssuer) {
        this.stsIssuer = stsIssuer;
    }

    public MagAuthTcuProps getTcu() {
        return this.tcu;
    }

    public void setTcu(final MagAuthTcuProps tcu) {
        this.tcu = tcu;
    }

    public static class MagAuthTcuProps {

        private String principalName;
        private String principalGln;
        private String keystorePath;
        private String keystorePassword;
        private String keystoreAlias;
        private String templatePath;
        private boolean autoInjectInIti65;

        public String getPrincipalName() {
            return this.principalName;
        }

        public void setPrincipalName(final String principalName) {
            this.principalName = principalName;
        }

        public String getPrincipalGln() {
            return this.principalGln;
        }

        public void setPrincipalGln(final String principalGln) {
            this.principalGln = principalGln;
        }

        public String getKeystorePath() {
            return this.keystorePath;
        }

        public void setKeystorePath(final String keystorePath) {
            this.keystorePath = keystorePath;
        }

        public String getKeystorePassword() {
            return this.keystorePassword;
        }

        public void setKeystorePassword(final String keystorePassword) {
            this.keystorePassword = keystorePassword;
        }

        public String getKeystoreAlias() {
            return this.keystoreAlias;
        }

        public void setKeystoreAlias(final String keystoreAlias) {
            this.keystoreAlias = keystoreAlias;
        }

        public String getTemplatePath() {
            return this.templatePath;
        }

        public void setTemplatePath(final String templatePath) {
            this.templatePath = templatePath;
        }

        public boolean isAutoInjectInIti65() {
            return this.autoInjectInIti65;
        }

        public void setAutoInjectInIti65(final boolean autoInjectInIti65) {
            this.autoInjectInIti65 = autoInjectInIti65;
        }
    }
}
