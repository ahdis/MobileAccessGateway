package ch.bfh.ti.i4mi.mag.common;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

public class UnknownPatientException extends InvalidRequestException {
    public UnknownPatientException(final String theMessage) {
        super(theMessage);
    }

    public UnknownPatientException(final String theMessage, final Throwable theCause) {
        super(theMessage, theCause);
    }
}
