package ch.bfh.ti.i4mi.mag.auth;

public enum PurposeOfUse {

    AUTO("Automatic Upload"),
    NORM("Normal Access"),
    EMER("Emergency Access");

    private final String display;

    PurposeOfUse(final String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
