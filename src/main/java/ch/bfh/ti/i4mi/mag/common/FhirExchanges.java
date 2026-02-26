package ch.bfh.ti.i4mi.mag.common;

import lombok.experimental.UtilityClass;
import org.apache.camel.Exchange;

import javax.annotation.Nullable;
import java.util.Map;

import static org.openehealth.ipf.platform.camel.ihe.ws.HeaderUtils.getIncomingHttpHeaders;

/**
 * A utility class for FHIR exchanges.
 *
 * @author Quentin Ligier
 **/
@UtilityClass
public class FhirExchanges {

    public static @Nullable String readRequestHttpHeader(final String headerName,
                                                         final Exchange exchange,
                                                         final boolean removeIfFound) {
        final Map<String, String> httpHeaders = getIncomingHttpHeaders(exchange);

        String value = null;
        if (httpHeaders != null) {
            value = httpHeaders.get(headerName);
            if (value != null && removeIfFound) {
                httpHeaders.remove(headerName);
            }
        } else {
            final Object authHeader = exchange.getMessage().getHeader(headerName);
            if (authHeader != null) {
                value = authHeader.toString();
                if (removeIfFound) {
                    exchange.getMessage().removeHeader(headerName);
                }
            }
        }
        return value;
    }
}
