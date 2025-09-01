package ch.bfh.ti.i4mi.mag.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mag.ppq")
public class MagPpqProps {

    private UrlProps pp1;
    private UrlProps pp2;
    private boolean chPpqmConstraints;

    public UrlProps getPp1() {
        return this.pp1;
    }

    public void setPp1(final UrlProps pp1) {
        this.pp1 = pp1;
    }

    public UrlProps getPp2() {
        return this.pp2;
    }

    public void setPp2(final UrlProps pp2) {
        this.pp2 = pp2;
    }

    public boolean isChPpqmConstraints() {
        return this.chPpqmConstraints;
    }

    public void setChPpqmConstraints(final boolean chPpqmConstraints) {
        this.chPpqmConstraints = chPpqmConstraints;
    }
}
