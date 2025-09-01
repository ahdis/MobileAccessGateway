package ch.bfh.ti.i4mi.mag.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mag.xds")
public class MagXdsProps {

    private boolean https;
    private String repositoryUniqueId;
    private UrlProps iti18;
    private UrlProps iti41;
    private UrlProps iti43;
    private UrlProps iti57;
    private UrlProps retrieve;
    private boolean chMhdContsraints;

    public boolean isHttps() {
        return this.https;
    }

    public void setHttps(final boolean https) {
        this.https = https;
    }

    public UrlProps getIti18() {
        return this.iti18;
    }

    public void setIti18(final UrlProps iti18) {
        this.iti18 = iti18;
    }

    public UrlProps getIti41() {
        return this.iti41;
    }

    public void setIti41(final UrlProps iti41) {
        this.iti41 = iti41;
    }

    public UrlProps getIti43() {
        return this.iti43;
    }

    public void setIti43(final UrlProps iti43) {
        this.iti43 = iti43;
    }

    public UrlProps getIti57() {
        return this.iti57;
    }

    public void setIti57(final UrlProps iti57) {
        this.iti57 = iti57;
    }

    public UrlProps getRetrieve() {
        return this.retrieve;
    }

    public void setRetrieve(final UrlProps retrieve) {
        this.retrieve = retrieve;
    }

    public String getRepositoryUniqueId() {
        return this.repositoryUniqueId;
    }

    public void setRepositoryUniqueId(final String repositoryUniqueId) {
        this.repositoryUniqueId = repositoryUniqueId;
    }

    public boolean isChMhdConstraints() {
        return this.chMhdContsraints;
    }

    public void setChMhdConstraints(final boolean chMhdConstraints) {
        this.chMhdContsraints = chMhdConstraints;
    }
}
