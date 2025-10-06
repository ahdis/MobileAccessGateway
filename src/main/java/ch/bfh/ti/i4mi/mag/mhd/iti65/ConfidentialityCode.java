package ch.bfh.ti.i4mi.mag.mhd.iti65;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

public enum ConfidentialityCode {
    NORMAL("17621005", "2.16.840.1.113883.6.96"),
    RESTRICTED("263856008", "2.16.840.1.113883.6.96"),
    SECRET("1141000195107", "2.16.756.5.30.1.127.3.4");

    private final String code;
    private final String system;

    ConfidentialityCode(final String code, final String system) {
        this.code = code;
        this.system = system;
    }

    public String getCode() {
        return this.code;
    }

    public String getSystem() {
        return this.system;
    }

    public static ConfidentialityCode from(final String code, final String system) {
        for (ConfidentialityCode level : ConfidentialityCode.values()) {
            if (level.code.equals(code) && level.system.equals(system)) {
                return level;
            }
        }
        throw new InvalidRequestException("Unknown confidentiality level code: " + code + " in system: " + system);
    }
}