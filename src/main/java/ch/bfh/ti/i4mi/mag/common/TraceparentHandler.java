package ch.bfh.ti.i4mi.mag.common;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static org.openehealth.ipf.platform.camel.ihe.ws.HeaderUtils.addOutgoingHttpHeaders;

@Service
public class TraceparentHandler {
    private static final Logger log = LoggerFactory.getLogger(TraceparentHandler.class);
    public static final String TRACEPARENT_HEADER = "traceparent";
    public static final String TRACEPARENT_CAMEL_HEADER = "MAG.Traceparent";

    private final boolean enabled;

    public TraceparentHandler(@Value("${mag.traceparent.enabled:true}") final boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            log.info("Traceparent header handling is enabled");
        } else {
            log.info("Traceparent header handling is disabled");
        }
    }

    public void saveHeader(final Exchange exchange) {
        if (!this.enabled) {
            return;
        }
        final var incomingTraceparent = FhirExchanges.readRequestHttpHeader(TRACEPARENT_HEADER, exchange, true);
        Traceparent parsedTraceparent = Traceparent.random();
        if (incomingTraceparent == null) {
            log.trace("No traceparent header found in the request, using a random one: {}", parsedTraceparent);
        } else {
            try {
                parsedTraceparent = Traceparent.parse(incomingTraceparent);
            } catch (final Exception e) {
                log.debug("Could not parse traceparent header: {}", incomingTraceparent);
            }
        }

        exchange.getMessage().setHeader(TRACEPARENT_CAMEL_HEADER, parsedTraceparent);
    }

    public Processor updateHeaderForSoap() {
        return exchange -> {
            final var traceparent = exchange.getMessage().getHeader(TRACEPARENT_CAMEL_HEADER, Traceparent.class);
            if (traceparent == null) {
                log.trace("No traceparent header found (updateHeaderForSoap)");
                return;
            }

            // We need to update the parentId value before forwarding it
            final var newTraceparent = traceparent.withRandomParentId();

            log.debug("Forwarding traceparent header: {} | The original was: {}", newTraceparent, traceparent);
            addOutgoingHttpHeaders(exchange, TRACEPARENT_HEADER, newTraceparent.toString());
        };
    }

    public Processor updateHeaderForFhir() {
        return exchange -> {
            final var traceparent = exchange.getMessage().getHeader(TRACEPARENT_CAMEL_HEADER, Traceparent.class);
            if (traceparent == null) {
                log.debug("No traceparent header found (updateHeaderForFhir)");
                return;
            }

            // We need to update the parentId value before forwarding it
            final var newTraceparent = traceparent.withRandomParentId();

            log.debug("Forwarding traceparent header: {} | The original was: {}", newTraceparent, traceparent);
            addOutgoingHttpHeaders(exchange, TRACEPARENT_HEADER, newTraceparent.toString());
        };
    }
}
