package ch.bfh.ti.i4mi.mag.common;

import ch.bfh.ti.i4mi.mag.Config;
import net.ihe.gazelle.hl7v3.datatypes.II;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.openehealth.ipf.commons.ihe.hl7v3.core.metadata.Device;
import org.openehealth.ipf.commons.ihe.hl7v3.core.requests.PixV3QueryRequest;
import org.openehealth.ipf.commons.ihe.hl7v3.core.responses.PixV3QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A service dedicated to patient identifiers cross-mapping: XAD-PID ↔ EPR-SPID.
 **/
@Service
public class PatientIdMappingService implements CamelContextAware {
    private static final Logger log = LoggerFactory.getLogger(PatientIdMappingService.class);

    // Map: keys are XAD-PIDs, values are EPR-SPIDs.
    private final Map<String, String> cache;

    private final String xadMpiOid;
    private final Device pixv3Sender;
    private final Device pixv3Receiver;

    private CamelContext camelContext;
    private ProducerTemplate producerTemplate;
    private final String pixv3Endpoint;

    public PatientIdMappingService(final Config config) {
        this.cache = new PassiveExpiringMap<>(5, TimeUnit.MINUTES);
        this.xadMpiOid = config.getOidMpiPid();
        this.pixv3Endpoint = "pixv3-iti45://%s?secure=%s&audit=%s".formatted(
                config.getIti45HostUrl(),
                String.valueOf(config.isPixHttps()),
                String.valueOf(true)
        );

        this.pixv3Sender = new Device();
        this.pixv3Sender.getIds().add(new II(config.getPixMySenderOid(), null));

        this.pixv3Receiver = new Device();
        this.pixv3Receiver.getIds().add(new II(config.getPixReceiverOid(), null));
    }

    @Override
    public CamelContext getCamelContext() {
        return this.camelContext;
    }

    @Override
    public void setCamelContext(final CamelContext camelContext) {
        this.camelContext = camelContext;
        this.producerTemplate = camelContext.createProducerTemplate();
    }

    public String getXadPid(final String eprSpid) throws Exception {
        final var maybeXadPid = this.cache.entrySet().parallelStream()
                .filter(entry -> eprSpid.equals(entry.getValue()))
                .findAny()
                .map(Map.Entry::getKey);
        if (maybeXadPid.isPresent()) {
            return maybeXadPid.get();
        }

        final var xadPid = this.query(new II(Config.OID_EPRSPID, eprSpid), this.xadMpiOid);
        this.cache.put(xadPid, eprSpid);
        return xadPid;
    }

    public String getEprSpid(final String xadPid) throws Exception {
        if (this.cache.containsKey(xadPid)) {
            return this.cache.get(xadPid);
        }

        final var eprSpid = this.query(new II(this.xadMpiOid, xadPid), Config.OID_EPRSPID);
        this.cache.put(xadPid, eprSpid);
        return eprSpid;
    }

    public void save(final String xadPid, final String eprSpid) {
        if (xadPid == null || eprSpid == null) {
            log.warn("Trying to save null patient identifier");
            return;
        }
        log.debug("PatientIdMappingService: caching {} with {}", xadPid, eprSpid);
        this.cache.put(xadPid, eprSpid);
    }

    private String query(final II queriedIdentifier,
                         final String wantedSystem) throws Exception {
        final var pixQuery = new PixV3QueryRequest();
        pixQuery.setQueryPatientId(queriedIdentifier);
        pixQuery.getDataSourceOids().add(Config.OID_EPRSPID);
        pixQuery.getDataSourceOids().add(this.xadMpiOid);
        pixQuery.setMessageId(new II(UUID.randomUUID().toString(), null));
        pixQuery.setQueryId(new II(UUID.randomUUID().toString(), null));
        pixQuery.setSender(this.pixv3Sender);
        pixQuery.setReceiver(this.pixv3Receiver);
        pixQuery.setCreationTime(ZonedDateTime.now());

        final var exchange = new DefaultExchange(getCamelContext());
        exchange.getIn().setBody(pixQuery);

        final Exchange result = this.producerTemplate.send(this.pixv3Endpoint, exchange);
        if (result.getException() != null) {
            log.warn("Error during PIX query", result.getException());
            throw result.getException();
        }
        final var pixResponse = result.getMessage().getBody(PixV3QueryResponse.class);
        for (final var identifier : pixResponse.getPatientIds()) {
            if (wantedSystem.equals(identifier.getRoot())) {
                return identifier.getExtension();
            }
        }
        throw new UnknownPatientException(
                "No patient identifier found for identifier %s|%s".formatted(queriedIdentifier.getRoot(),
                                                                             queriedIdentifier.getExtension())
        );
    }
}
