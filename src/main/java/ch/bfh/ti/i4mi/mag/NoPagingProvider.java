package ch.bfh.ti.i4mi.mag;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IPagingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoPagingProvider implements IPagingProvider {
    private static final Logger logger = LoggerFactory.getLogger(NoPagingProvider.class);

    @Override
    public IBundleProvider retrieveResultList(RequestDetails theRequestDetails, String theId) {
        logger.info("Attempting to retrieve result list with ID: {}", theId);
        logger.debug("No paging is applied, returning null");
        return null; // Or implement to return all results at once
    }

    @Override
    public String storeResultList(RequestDetails theRequestDetails, IBundleProvider theList) {
        logger.info("Attempting to store result list");
        logger.debug("No paging is applied, returning null");
        return null; // Or implement to return all results without paging
    }

    @Override
    public int getDefaultPageSize() {
        logger.debug("Default page size requested, returning Integer.MAX_VALUE");
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaximumPageSize() {
        logger.debug("Maximum page size requested, returning Integer.MAX_VALUE");
        return Integer.MAX_VALUE;
    }
}