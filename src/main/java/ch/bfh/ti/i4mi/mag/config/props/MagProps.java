package ch.bfh.ti.i4mi.mag.config.props;


import org.openehealth.ipf.boot.atna.IpfAtnaConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mag")
public class MagProps {

    private String homeCommunityId;
    private String baseUrl;
    private String documentSourceId;

    private MagClientSslProps clientSsl;
    private MagXdsProps xds;
    private MagMpiProps mpi;
    private MagPpqProps ppq;
    private IpfAtnaConfigurationProperties audit;
    private MagAuthProps auth;

    public String getHomeCommunityId() {
        return this.homeCommunityId;
    }

    public void setHomeCommunityId(final String homeCommunityId) {
        this.homeCommunityId = homeCommunityId;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public void setBaseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDocumentSourceId() {
        return this.documentSourceId;
    }

    public void setDocumentSourceId(final String documentSourceId) {
        this.documentSourceId = documentSourceId;
    }

    public MagClientSslProps getClientSsl() {
        return this.clientSsl;
    }

    public void setClientSsl(final MagClientSslProps clientSsl) {
        this.clientSsl = clientSsl;
    }

    public MagXdsProps getXds() {
        return this.xds;
    }

    public void setXds(final MagXdsProps xds) {
        this.xds = xds;
    }

    public MagMpiProps getMpi() {
        return this.mpi;
    }

    public void setMpi(final MagMpiProps mpi) {
        this.mpi = mpi;
    }

    public MagPpqProps getPpq() {
        return this.ppq;
    }

    public void setPpq(final MagPpqProps ppq) {
        this.ppq = ppq;
    }

    public IpfAtnaConfigurationProperties getAudit() {
        return this.audit;
    }

    public void setAudit(final IpfAtnaConfigurationProperties audit) {
        this.audit = audit;
    }

    public MagAuthProps getAuth() {
        return this.auth;
    }

    public void setAuth(final MagAuthProps auth) {
        this.auth = auth;
    }

    public String getFhirBaseUrl() {
        return this.baseUrl + "/fhir";
    }
}
