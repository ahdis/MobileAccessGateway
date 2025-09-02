package ch.bfh.ti.i4mi.mag.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mag.auth")
public class MagAuthProps {

    private String sts;
    private String stsWsdl;

    public String getSts() {
        return this.sts;
    }

    public void setSts(final String sts) {
        this.sts = sts;
    }

    public String getStsWsdl() {
        return this.stsWsdl;
    }

    public void setStsWsdl(final String stsWsdl) {
        this.stsWsdl = stsWsdl;
    }
}
