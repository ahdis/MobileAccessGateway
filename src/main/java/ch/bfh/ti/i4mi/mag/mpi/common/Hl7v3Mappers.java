package ch.bfh.ti.i4mi.mag.mpi.common;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import jakarta.annotation.Nullable;
import net.ihe.gazelle.hl7v3.datatypes.*;
import net.ihe.gazelle.hl7v3.mccimt000300UV01.MCCIMT000300UV01Acknowledgement;
import net.ihe.gazelle.hl7v3.mccimt000300UV01.MCCIMT000300UV01AcknowledgementDetail;
import org.hl7.fhir.r4.model.*;

import java.io.Serializable;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ch.bfh.ti.i4mi.mag.common.JavaUtils.firstOrNull;
import static ch.bfh.ti.i4mi.mag.mpi.common.FhirExceptions.*;

public class Hl7v3Mappers {

    // Constants for the "Patient ID unknown" case (PIU)
    private static final String PIU_ITH_AHV_PREFIX = "No patient found with";
    private static final Pattern PIU_ITH_PATTERN =
            Pattern.compile("The ID number \"[^\"]+\" is not recognized by the MPI");
    private static final String PIU_EMEDO_PREFIX = "Requested record not found";

    /**
     * This class is not instantiable.
     */
    private Hl7v3Mappers() {
    }

    public static String toText(final ANY in) {
        final var result = new StringBuilder();
        if (in.mixed == null) {
            return "";
        }
        for (final Serializable obj : in.mixed) {
            if (obj instanceof final String string) {
                result.append(string);
            }
        }
        return result.toString();
    }

    public static void verifyAck(final CS queryResponseCode,
                                 final List<MCCIMT000300UV01Acknowledgement> acknowledgements,
                                 final String transactionName) {
        final var code = queryResponseCode.getCode();
        if ("NF".equals(code)) {
            throw targetSystemNotFound();
        } else if ("AE".equals(code)) {
            final var errorMessages = acknowledgements.stream()
                    .map(MCCIMT000300UV01Acknowledgement::getAcknowledgementDetail)
                    .flatMap(List::stream)
                    .map(MCCIMT000300UV01AcknowledgementDetail::getText)
                    .map(BIN::getListStringValues)
                    .flatMap(List::stream)
                    .collect(Collectors.joining());
            if (errorMessages.contains(PIU_ITH_AHV_PREFIX) || errorMessages.contains(PIU_EMEDO_PREFIX) || PIU_ITH_PATTERN.matcher(errorMessages).find()) {
                throw sourceIdentifierNotFound();
            }

            throw sourceAssigningAuthorityNotFound();
        } else if (!"OK".equals(code)) {
            throw new InternalErrorException(
                    "Unknown queryResponseCode in %s response: %s".formatted(transactionName, queryResponseCode));
        }
    }

    @Nullable
    public static Period transform(final @Nullable IVLTS period) {
        if (period == null) {
            return null;
        }
        final var result = new Period();
        if (period.getLow() != null) {
            result.setStartElement(transform(period.getLow()));
        }
        if (period.getHigh() != null) {
            result.setEndElement(transform(period.getHigh()));
        }
        return result;
    }

    public static DateTimeType transform(final IVXBTS date) {
        return DateTimeType.parseV3(date.getValue());
    }

    @Nullable
    public static DateType transform(final @Nullable TS date) {
        if (date == null) return null;
        return DateType.parseV3(date.getValue());
    }

    public static <T extends ENXP> StringType withQualifier(T namePart, StringType fhirNamePart) {
        if (namePart.getQualifier() != null) {
            fhirNamePart.addExtension("http://hl7.org/fhir/StructureDefinition/iso21090-EN-qualifier", new StringType(namePart.getQualifier()));
        }
        fhirNamePart.setValue(val(namePart));
        return fhirNamePart;
    }

    public static Address transform(final AD address) {
        Address addr = new Address();
        addr.setCity(val(address.getCity()));
        addr.setCountry(val(address.getCountry()));
        addr.setDistrict(val(address.getCounty()));
        addr.setPostalCode(val(address.getPostalCode()));
        addr.setState(val(address.getState()));
        for (AdxpStreetName street : address.getStreetName()) {
            final var streetValue = val(street);
            if (streetValue != null)
                addr.addLine(streetValue);
        }
        // TODO Missing: type, use
        for (AdxpStreetAddressLine line : address.getStreetAddressLine()) {
            final var lineValue = val(line);
            if (lineValue != null)
                addr.addLine(lineValue);
        }
        if (address.getUseablePeriod() != null) {
            //addr.setPeriod(transform(address.getUseablePeriod().get(0)));
        }
        if (address.getUse() != null) {
            switch (address.getUse()) {
                case "H":addr.setUse(Address.AddressUse.HOME);break;
                case "WP":addr.setUse(Address.AddressUse.WORK);break;
                case "TMP":addr.setUse(Address.AddressUse.TEMP);break;
                case "OLD":addr.setUse(Address.AddressUse.OLD);break;
            }
        }

        return addr;
    }

    public static HumanName transform(final PN name) {
        final var humanName = new HumanName();
        for (EnFamily family : name.getFamily()) {
            if ("BR".equals(family.getQualifier())) {
                humanName.setUse(HumanName.NameUse.MAIDEN);
            }
            humanName.setFamily(val(family));
        }
        for (EnGiven given : name.getGiven()) {
            withQualifier(given, humanName.addGivenElement());
        }
        for (EnPrefix prefix : name.getPrefix()) {
            withQualifier(prefix, humanName.addPrefixElement());
        }
        for (EnSuffix suffix : name.getSuffix()) {
            withQualifier(suffix, humanName.addSuffixElement());
        }
        if (name.getValidTime() != null) humanName.setPeriod(transform(name.getValidTime()));
        return humanName;
    }

    public static ContactPoint transform(TEL telecom) {
        ContactPoint contactPoint = new ContactPoint();

        String use = telecom.getUse();
        if (use != null) {
            switch (use) {
                case "H":
                case "HP":
                case "HV":
                    contactPoint.setUse(ContactPoint.ContactPointUse.HOME);
                    break;
                case "WP":
                case "DIR":
                case "PUB":
                    contactPoint.setUse(ContactPoint.ContactPointUse.WORK);
                    break;
                case "TMP":
                    contactPoint.setUse(ContactPoint.ContactPointUse.TEMP);
                    break;
                case "MC":
                    contactPoint.setUse(ContactPoint.ContactPointUse.MOBILE);
                    break;
            }
        }
        final String[] telecomParts = telecom.getValue().split(":");
        if (telecomParts.length == 2) {
            switch (telecomParts[0].toLowerCase()) {
                case "mailto":
                    contactPoint.setSystem(ContactPoint.ContactPointSystem.EMAIL);
                    break;
                case "tel":
                    contactPoint.setSystem(ContactPoint.ContactPointSystem.PHONE);
                    break;
                case "fax":
                    contactPoint.setSystem(ContactPoint.ContactPointSystem.FAX);
                    break;
            }
            if (contactPoint.getSystem() == null) {
                // No system, no contactPoint
                return null;
            }
            contactPoint.setValue(telecomParts[1]);
            return contactPoint;
        }
        else {
            if (telecom.getValue().contains("@")) {
                contactPoint.setSystem(ContactPoint.ContactPointSystem.EMAIL);
            }
            contactPoint.setValue(telecom.getValue());
            return contactPoint;
        }
    }

    @Nullable
    public static String val(final ST in) {
        return firstOrNull(in.getListStringValues());
    }

    @Nullable
    public static String val(final @Nullable List<? extends ST> in) {
        if (in == null || in.isEmpty()) return null;
        final var sb = new StringBuilder();
        for (final ST st : in) {
            final var stValue = val(st);
            if (stValue != null) {
                if (!sb.isEmpty())
                    sb.append(" ");
                sb.append(stValue);
            }
        }
        if (sb.isEmpty())
            return null;
        return sb.toString();
    }
}
