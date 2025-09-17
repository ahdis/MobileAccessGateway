package ch.bfh.ti.i4mi.mag.config.props;

public class MagHpdProps {

    private boolean https;
    private String iti58;
    private boolean chMcsdConstraints;

    public boolean isHttps() {
        return this.https;
    }

    public void setHttps(final boolean https) {
        this.https = https;
    }

    public String getIti58() {
        return this.iti58;
    }

    public void setIti58(final String iti58) {
        this.iti58 = iti58;
    }

    public boolean isChMcsdConstraints() {
        return this.chMcsdConstraints;
    }

    public void setChMcsdConstraints(final boolean chMcsdConstraints) {
        this.chMcsdConstraints = chMcsdConstraints;
    }
}
