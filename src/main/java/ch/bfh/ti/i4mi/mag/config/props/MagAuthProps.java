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

        private String generatorUrl;
        private String principalName;
        private String principalGln;

        public String getGeneratorUrl() {
            return this.generatorUrl;
        }

        public void setGeneratorUrl(final String generatorUrl) {
            this.generatorUrl = generatorUrl;
        }

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
    }
}
