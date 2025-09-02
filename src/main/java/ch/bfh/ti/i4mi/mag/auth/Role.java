package ch.bfh.ti.i4mi.mag.auth;

public enum Role {

    HCP("Healthcare professional"),
    TCU("Technical user");

    private final String display;

    Role(final String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
