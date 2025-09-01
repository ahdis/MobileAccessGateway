package ch.bfh.ti.i4mi.mag.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mag.mpi")
public class MagMpiProps {

    private boolean https;
    private UrlProps iti44;
    private UrlProps iti45;
    private UrlProps iti47;
    private MagMpiOidsProps oids;
    private boolean chPixmConstraints;
    private boolean chPdqmConstraints;
    private boolean chEprspidAsPatientId;
    private String localPatientIDAssigningAuthority;

    public boolean isHttps() {
        return this.https;
    }

    public void setHttps(final boolean https) {
        this.https = https;
    }

    public UrlProps getIti44() {
        return this.iti44;
    }

    public void setIti44(final UrlProps iti44) {
        this.iti44 = iti44;
    }

    public UrlProps getIti45() {
        return this.iti45;
    }

    public void setIti45(final UrlProps iti45) {
        this.iti45 = iti45;
    }

    public UrlProps getIti47() {
        return this.iti47;
    }

    public void setIti47(final UrlProps iti47) {
        this.iti47 = iti47;
    }

    public MagMpiOidsProps getOids() {
        return this.oids;
    }

    public void setOids(final MagMpiOidsProps oids) {
        this.oids = oids;
    }

    public boolean isChPixmConstraints() {
        return this.chPixmConstraints;
    }

    public void setChPixmConstraints(final boolean chPixmConstraints) {
        this.chPixmConstraints = chPixmConstraints;
    }

    public boolean isChPdqmConstraints() {
        return this.chPdqmConstraints;
    }

    public void setChPdqmConstraints(final boolean chPdqmConstraints) {
        this.chPdqmConstraints = chPdqmConstraints;
    }

    public boolean isChEprspidAsPatientId() {
        return this.chEprspidAsPatientId;
    }

    public void setChEprspidAsPatientId(final boolean chEprspidAsPatientId) {
        this.chEprspidAsPatientId = chEprspidAsPatientId;
    }

    public String getLocalPatientIDAssigningAuthority() {
        return this.localPatientIDAssigningAuthority;
    }

    public void setLocalPatientIDAssigningAuthority(final String localPatientIDAssigningAuthority) {
        this.localPatientIDAssigningAuthority = localPatientIDAssigningAuthority;
    }

    @ConfigurationProperties(prefix = "mag.mpi.oids")
    public static class MagMpiOidsProps {

        private String sender;
        private String receiver;
        private String custodian;
        private String mpiPid;

        public String getSender() {
            return this.sender;
        }

        public void setSender(final String sender) {
            this.sender = sender;
        }

        public String getReceiver() {
            return this.receiver;
        }

        public void setReceiver(final String receiver) {
            this.receiver = receiver;
        }

        public String getCustodian() {
            return this.custodian;
        }

        public void setCustodian(final String custodian) {
            this.custodian = custodian;
        }

        public String getMpiPid() {
            return this.mpiPid;
        }

        public void setMpiPid(final String mpiPid) {
            this.mpiPid = mpiPid;
        }
    }
}
