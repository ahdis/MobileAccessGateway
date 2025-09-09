package ch.bfh.ti.i4mi.mag.mpi.common;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

public class FhirExceptions {
    /**
     * This class is not instantiable.
     */
    private FhirExceptions() {
    }

    public static BaseServerResponseException targetSystemNotFound() {
        return new ForbiddenOperationException("targetSystem not found");
    }

    public static BaseServerResponseException sourceIdentifierNotFound() {
        return new ResourceNotFoundException("sourceIdentifier Patient Identifier not found");
    }

    public static BaseServerResponseException sourceAssigningAuthorityNotFound() {
        return new InvalidRequestException("sourceIdentifier Assigning Authority not found");
    }
}
