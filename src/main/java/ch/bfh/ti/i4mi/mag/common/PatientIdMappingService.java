package ch.bfh.ti.i4mi.mag.common;

import ch.bfh.ti.i4mi.mag.config.props.MagMpiProps;
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

import static ch.bfh.ti.i4mi.mag.MagConstants.EPR_SPID_OID;

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

    public PatientIdMappingService(final MagMpiProps mpiProps) {
        this.cache = new PassiveExpiringMap<>(5, TimeUnit.MINUTES);
        this.xadMpiOid = mpiProps.getOids().getMpiPid();
        this.pixv3Endpoint = "pixv3-iti45://%s?secure=%s&audit=%s".formatted(
                mpiProps.getIti45(),
                String.valueOf(mpiProps.isHttps()),
                String.valueOf(true)
        );

        this.pixv3Sender = new Device();
        this.pixv3Sender.getIds().add(new II(mpiProps.getOids().getSender(), null));

        this.pixv3Receiver = new Device();
        this.pixv3Receiver.getIds().add(new II(mpiProps.getOids().getReceiver(), null));
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
            log.trace("PatientIdMappingService: cache hit for EPR-SPID={}: XAD-PID={}", eprSpid, maybeXadPid.get());
            return maybeXadPid.get();
        }

        log.trace("PatientIdMappingService: cache miss for EPR-SPID={}", eprSpid);
        final var xadPid = this.query(new II(EPR_SPID_OID, eprSpid), this.xadMpiOid);
        log.trace("PatientIdMappingService: caching XAD-PID={} with EPR-SPID={}", xadPid, eprSpid);
        this.cache.put(xadPid, eprSpid);
        return xadPid;
    }

    public String getEprSpid(final String xadPid) throws Exception {
        if (this.cache.containsKey(xadPid)) {
            log.trace("PatientIdMappingService: cache hit for XAD-PID={}: EPR-SPID={}", xadPid, this.cache.get(xadPid));
            return this.cache.get(xadPid);
        }

        log.trace("PatientIdMappingService: cache miss for XAD-PID={}", xadPid);
        final var eprSpid = this.query(new II(this.xadMpiOid, xadPid), EPR_SPID_OID);
        log.trace("PatientIdMappingService: caching EPR-SPID={} with XAD-PID={}", eprSpid, xadPid);
        this.cache.put(xadPid, eprSpid);
        return eprSpid;
    }

    public void save(final String xadPid, final String eprSpid) {
        if (xadPid == null || eprSpid == null) {
            log.warn("Trying to save null patient identifier");
            return;
        }
        log.trace("PatientIdMappingService: caching XAD-PID={} with EPR-SPID={}", xadPid, eprSpid);
        this.cache.put(xadPid, eprSpid);
    }

    private String query(final II queriedIdentifier,
                         final String wantedSystem) throws Exception {
        log.debug("PatientIdMappingService: sending PIX query for identifier {}|{} to retrieve identifier with system {}",
                  queriedIdentifier.getRoot(), queriedIdentifier.getExtension(), wantedSystem);
        final var pixQuery = new PixV3QueryRequest();
        pixQuery.setQueryPatientId(queriedIdentifier);
        pixQuery.getDataSourceOids().add(EPR_SPID_OID);
        pixQuery.getDataSourceOids().add(this.xadMpiOid);
        pixQuery.setMessageId(new II(UUID.randomUUID().toString(), null));
        pixQuery.setQueryId(new II(UUID.randomUUID().toString(), null));
        pixQuery.setSender(this.pixv3Sender);
        pixQuery.setReceiver(this.pixv3Receiver);
        pixQuery.setCreationTime(ZonedDateTime.now());

        final var exchange = new DefaultExchange(getCamelContext());
        exchange.getIn().setBody(pixQuery);
        log.trace("PatientIdMappingService: sending PIX query to endpoint {}", this.pixv3Endpoint);
        log.trace(exchange.getIn().getBody(String.class));

        final Exchange result = this.producerTemplate.send(this.pixv3Endpoint, exchange);
        if (result.getException() != null) {
            log.warn("Error during PIX query", result.getException());
            throw result.getException();
        }
        log.trace("PatientIdMappingService: received PIX query response");
        log.trace(result.getMessage().getBody(String.class));
        final var pixResponse = result.getMessage().getBody(PixV3QueryResponse.class);
        for (final var identifier : pixResponse.getPatientIds()) {
            if (wantedSystem.equals(identifier.getRoot())) {
                log.trace("PatientIdMappingService: found identifier {}|{} for system {} in PIX query response",
                          identifier.getRoot(), identifier.getExtension(), wantedSystem);
                return identifier.getExtension();
            }
        }
        throw new UnknownPatientException(
                "No patient identifier found for identifier %s|%s".formatted(queriedIdentifier.getRoot(),
                                                                             queriedIdentifier.getExtension())
        );
    }
}
