package ch.bfh.ti.i4mi.mag.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mag.xds")
public class MagXdsProps {

    private boolean https;
    private String repositoryUniqueId;
    private String iti18;
    private String xca38;
    private String xca39;
    private String iti41;
    private String iti43;
    private String iti57;
    private String retrieve;
    private boolean chMhdConstraints;

    public boolean isHttps() {
        return this.https;
    }

    public void setHttps(final boolean https) {
        this.https = https;
    }

    public String getIti18() {
        return this.iti18;
    }

    public void setIti18(final String iti18) {
        this.iti18 = iti18;
    }

    public String getXca18() {
        return this.xca38;
    }

    public void setXca18(final String xca18) {
        this.xca38 = xca18;
    }

    public String getIti41() {
        return this.iti41;
    }

    public void setIti41(final String iti41) {
        this.iti41 = iti41;
    }

    public String getIti43() {
        return this.iti43;
    }

    public void setIti43(final String iti43) {
        this.iti43 = iti43;
    }

    public String getXca43() {
        return this.xca39;
    }

    public void setXca43(final String xca43) {
        this.xca39 = xca43;
    }

    public String getIti57() {
        return this.iti57;
    }

    public void setIti57(final String iti57) {
        this.iti57 = iti57;
    }

    public String getRetrieve() {
        return this.retrieve;
    }

    public void setRetrieve(final String retrieve) {
        this.retrieve = retrieve;
    }

    public String getRepositoryUniqueId() {
        return this.repositoryUniqueId;
    }

    public void setRepositoryUniqueId(final String repositoryUniqueId) {
        this.repositoryUniqueId = repositoryUniqueId;
    }

    public boolean isChMhdConstraints() {
        return this.chMhdConstraints;
    }

    public void setChMhdConstraints(final boolean chMhdConstraints) {
        this.chMhdConstraints = chMhdConstraints;
    }
}
