package ch.bfh.ti.i4mi.mag.mcsd.iti90;

import ch.bfh.ti.i4mi.mag.BaseRequestConverter;
import ch.bfh.ti.i4mi.mag.mhd.SchemeMapper;

/**
 * MobileAccessGateway
 *
 * @author Quentin Ligier
 **/
public class Iti90RequestConverter extends BaseRequestConverter {

    public Iti90RequestConverter(final SchemeMapper schemeMapper) {
        super(schemeMapper);
    }
/*
    public static void convert(@Body final Iti90EndpointSearchParameters sp) {
        throw new IllegalArgumentException("The Endpoint resource is not supported");
    }

    public static void convert(@Body final Iti90HealthcareServiceSearchParameters sp) {
        throw new IllegalArgumentException("The HealthcareService resource is not supported");
    }

    public static void convert(@Body final Iti90LocationSearchParameters sp) {
        throw new IllegalArgumentException("The Location resource is not supported");
    }

    public static void convert(@Body final Iti90OrganizationAffiliationSearchParameters sp) {
        throw new IllegalArgumentException("The OrganizationAffiliation resource is not supported");
    }

    public static BatchRequest convert(@Body final Iti90OrganizationSearchParameters sp) {
    }

    public static BatchRequest convert(@Body final Iti90PractitionerRoleSearchParameters sp) {
    }

    public static BatchRequest convert(@Body final Iti90PractitionerSearchParameters sp) {
    }*/
}
