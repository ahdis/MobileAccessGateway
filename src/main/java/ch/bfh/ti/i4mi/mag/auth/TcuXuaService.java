package ch.bfh.ti.i4mi.mag.auth;

import ch.bfh.ti.i4mi.mag.config.props.MagAuthProps;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A service for TCU XUA tokens.
 **/
@Service
@ConditionalOnProperty({
        "mag.auth.tcu.principal-name",
        "mag.auth.tcu.principal-gln",
        "mag.auth.tcu.keystore-path",
        "mag.auth.tcu.keystore-password",
        "mag.auth.tcu.keystore-alias",
        "mag.auth.tcu.template-path"
})
public class TcuXuaService {
    private static final Logger log = LoggerFactory.getLogger(TcuXuaService.class);

    private final MagAuthProps authProps;
    private final StsService stsService;
    private final Map<String, String> cachedTokens = new PassiveExpiringMap<>(4, TimeUnit.MINUTES);
    private final TcuAssertionGenerator tcuAssertionGenerator;

    public TcuXuaService(final MagAuthProps authProps,
                         final StsService stsService,
                         final TcuAssertionGenerator tcuAssertionGenerator) {
        this.authProps = authProps;
        this.stsService = stsService;
        this.tcuAssertionGenerator = tcuAssertionGenerator;
    }

    public String getXuaToken(final String eprSpid) {
        if (this.cachedTokens.containsKey(eprSpid)) {
            return this.cachedTokens.get(eprSpid);
        }

        final String tcuToken;
        try {
            tcuToken = this.tcuAssertionGenerator.generateNew();
        } catch (final Exception e) {
            log.debug("Failed to generate TCU token", e);
            throw new RuntimeException("Failed to generate TCU token", e);
        }

        final String xuaToken;
        try {
            xuaToken = this.stsService.requestXua(
                    eprSpid,
                    PurposeOfUse.AUTO,
                    Role.TCU,
                    this.authProps.getTcu().getPrincipalName(),
                    this.authProps.getTcu().getPrincipalGln(),
                    tcuToken
            );
        } catch (final Exception e) {
            throw new RuntimeException("Failed to fetch XUA token", e);
        }
        log.trace("Got XUA token for EPR-SPID {}: {}", eprSpid, xuaToken);

        this.cachedTokens.put(eprSpid, xuaToken);
        return xuaToken;
    }
}
