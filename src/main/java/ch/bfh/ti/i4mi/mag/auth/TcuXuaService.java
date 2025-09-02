package ch.bfh.ti.i4mi.mag.auth;

import ch.bfh.ti.i4mi.mag.config.props.MagAuthProps;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A service for TCU XUA tokens.
 **/
@Service
public class TcuXuaService {

    private final MagAuthProps authProps;
    private final StsService stsService;
    private final Map<String, Element> cachedTokens = new PassiveExpiringMap<>(4, TimeUnit.MINUTES);

    public TcuXuaService(final MagAuthProps authProps,
                         final StsService stsService) {
        this.authProps = authProps;
        this.stsService = stsService;
    }

    public Element getXuaToken(final String eprSpid) {
        if (this.cachedTokens.containsKey(eprSpid)) {
            return this.cachedTokens.get(eprSpid);
        }

        final var tcuToken = this.fetchTcuToken();

        final var xuaToken = this.stsService.requestXua(
                eprSpid,
                PurposeOfUse.AUTO,
                Role.TCU,
                this.authProps.getTcu().getPrincipalName(),
                this.authProps.getTcu().getPrincipalGln(),
                tcuToken
        );
        this.cachedTokens.put(eprSpid, xuaToken);
        return xuaToken;
    }

    private String fetchTcuToken() throws URISyntaxException, IOException, InterruptedException {
        final var client = HttpClient.newHttpClient();
        final var request = HttpRequest.newBuilder(new URI(this.authProps.getTcu().getGeneratorUrl()))
                .header("User-Agent", "MobileAccessGateway")
                .header("Accept", "text/xml")
                .GET()
                .build();
        final var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
