package ch.bfh.ti.i4mi.mag.auth;

import ch.bfh.ti.i4mi.mag.config.props.MagAuthProps;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A service for TCU XUA tokens.
 **/
@Service
@ConditionalOnProperty({"mag.auth.tcu.principal-name", "mag.auth.tcu.principal-gln", "mag.auth.tcu.generator-url"})
public class TcuXuaService implements CamelContextAware {
    private static final Logger log = LoggerFactory.getLogger(TcuXuaService.class);

    private final MagAuthProps authProps;
    private final StsService stsService;
    private final Map<String, String> cachedTokens = new PassiveExpiringMap<>(4, TimeUnit.MINUTES);
    private CamelContext camelContext;
    private ProducerTemplate producerTemplate;
    private final SSLContext sslContext;

    public TcuXuaService(final MagAuthProps authProps,
                         final StsService stsService,
                         final SSLContext sslContext) {
        this.authProps = authProps;
        this.stsService = stsService;
        this.sslContext = sslContext;
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

    public String getXuaToken(final String eprSpid) {
        if (this.cachedTokens.containsKey(eprSpid)) {
            return this.cachedTokens.get(eprSpid);
        }

        final String tcuToken;
        try {
            tcuToken = this.fetchTcuToken();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to fetch TCU token", e);
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

        this.cachedTokens.put(eprSpid, xuaToken);
        return xuaToken;
    }

    private String fetchTcuToken() throws Exception {
        final var client = HttpClients.custom()
                .setSSLSocketFactory(new SSLConnectionSocketFactory(
                                             SSLContexts.custom()
                                                     .loadTrustMaterial(null, new TrustAllStrategy()).build()
                                     )
                ).build();
        final var request = new HttpGet(this.authProps.getTcu().getGeneratorUrl());
        request.addHeader("User-Agent", "MobileAccessGateway");
        request.addHeader("Accept", "application/xml");
        return client.execute(request, httpResponse -> {
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                throw new IOException("Failed to fetch TCU token, status code: " + httpResponse.getStatusLine().getStatusCode());
            }
            return new String(httpResponse.getEntity().getContent().readAllBytes());
        });
        /*final var client = HttpClient.newBuilder()
                .sslContext(this.sslContext)
                .build();
        final var request = HttpRequest.newBuilder(new URI(this.authProps.getTcu().getGeneratorUrl()))
                .header("User-Agent", "MobileAccessGateway")
                .header("Accept", "application/xml")
                .GET()
                .build();
        final var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();*/
    }
}
