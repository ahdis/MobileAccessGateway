package ch.bfh.ti.i4mi.mag.config.props;

import jakarta.annotation.Nullable;
import org.openehealth.ipf.boot.atna.IpfAtnaConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mag")
public class MagProps {

    private String homeCommunityId;
    private String baseUrl;
    private String documentSourceId;
    private String organizationName;

    private MagClientSslProps clientSsl;
    private MagXdsProps xds;
    private MagMpiProps mpi;
    private MagPpqProps ppq;
    private MagHpdProps hpd;
    private MagAuthProps auth;

    @Autowired
    private IpfAtnaConfigurationProperties atna;

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

    public String getOrganizationName() {
        return this.organizationName;
    }

    public void setOrganizationName(final String organizationName) {
        this.organizationName = organizationName;
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

    public MagHpdProps getHpd() {
        return this.hpd;
    }

    public void setHpd(final MagHpdProps hpd) {
        this.hpd = hpd;
    }

    public MagAuthProps getAuth() {
        return this.auth;
    }

    public void setAuth(final MagAuthProps auth) {
        this.auth = auth;
    }

    public IpfAtnaConfigurationProperties getAtna() {
        return this.atna;
    }

    public void setAtna(final IpfAtnaConfigurationProperties atna) {
        this.atna = atna;
    }

    public String getFhirBaseUrl() {
        return this.baseUrl + "/fhir";
    }

    @Override
    public String toString() {
        return "MagProps{" +
                "homeCommunityId='" + homeCommunityId + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", documentSourceId='" + documentSourceId + '\'' +
                ", organizationName='" + organizationName + '\'' +
                ", clientSsl=" + clientSsl +
                ", xds=" + xds +
                ", mpi=" + mpi +
                ", ppq=" + ppq +
                ", hpd=" + hpd +
                ", auth=" + auth +
                ", atna=" + atnaToString(atna) +
                '}';
    }

    private static String atnaToString(final @Nullable IpfAtnaConfigurationProperties atna) {
        if (atna == null) {
            return "null";
        }
        return "IpfAtnaConfigurationProperties{" +
                "auditEnabled=" + atna.isAuditEnabled() +
                ", auditSourceType='" + atna.getAuditSourceType() + '\'' +
                ", auditSourceId='" + atna.getAuditSourceId() + '\'' +
                ", auditSendingApplication='" + atna.getAuditSendingApplication() + '\'' +
                ", auditRepositoryTransport='" + atna.getAuditRepositoryTransport() + '\'' +
                ", auditRepositoryHost='" + atna.getAuditRepositoryHost() + '\'' +
                ", auditRepositoryPort=" + atna.getAuditRepositoryPort() +
                ", auditEnterpriseSiteId='" + atna.getAuditEnterpriseSiteId() + '\'' +
                ", auditQueueClass='" + atna.getAuditQueueClass() + '\'' +
                ", auditMessagePostProcessorClass='" + atna.getAuditMessagePostProcessorClass() + '\'' +
                ", auditSenderClass='" + atna.getAuditSenderClass() + '\'' +
                ", auditExceptionHandlerClass='" + atna.getAuditExceptionHandlerClass() + '\'' +
                ", includeParticipantsFromResponse=" + atna.isIncludeParticipantsFromResponse() +
                ", auditValueIfMissing='" + atna.getAuditValueIfMissing() + '\'' +
                ", wsAuditDatasetEnricherClass='" + atna.getWsAuditDatasetEnricherClass() + '\'' +
                ", fhirAuditDatasetEnricherClass='" + atna.getFhirAuditDatasetEnricherClass() + '\'' +
                ", balp='" + atna.getBalp() + '\'' +
                '}';
    }
}
