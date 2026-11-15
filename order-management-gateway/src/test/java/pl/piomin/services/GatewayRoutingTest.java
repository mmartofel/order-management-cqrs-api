package pl.piomin.services;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * The gateway in front of three stub services that answer with their name, the method and the Authorization header
 * they received. The reference service's readiness answers 503, so its health component is down.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class GatewayRoutingTest {

    static final HttpServer COMMAND = stub("command", 200);
    static final HttpServer QUERY = stub("query", 200);
    static final HttpServer REFERENCE = stub("reference", 503);

    @Autowired
    RestTestClient client;

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("services.command-url", () -> url(COMMAND));
        registry.add("services.query-url", () -> url(QUERY));
        registry.add("services.reference-url", () -> url(REFERENCE));
    }

    @AfterAll
    static void stopStubs() {
        COMMAND.stop(0);
        QUERY.stop(0);
        REFERENCE.stop(0);
    }

    @Test
    void reads_goToTheQueryService() {
        client.get().uri("/api/orders?status=PAID").header(HttpHeaders.AUTHORIZATION, "Bearer token-1").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("query GET /api/orders?status=PAID Bearer token-1");
        client.get().uri("/api/deliveries/42").exchange()
                .expectBody(String.class).isEqualTo("query GET /api/deliveries/42 -");
    }

    @Test
    void writes_goToTheCommandService() {
        client.post().uri("/api/orders").header(HttpHeaders.AUTHORIZATION, "Bearer token-2")
                .contentType(MediaType.APPLICATION_JSON).body("{}").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("command POST /api/orders Bearer token-2");
        client.post().uri("/api/payments/7/complete").contentType(MediaType.APPLICATION_JSON).body("{}").exchange()
                .expectBody(String.class).isEqualTo("command POST /api/payments/7/complete -");
    }

    @Test
    void referenceData_goesToTheReferenceServiceWhateverTheMethod() {
        client.get().uri("/api/persons").exchange()
                .expectBody(String.class).isEqualTo("reference GET /api/persons -");
        client.put().uri("/api/items/3").contentType(MediaType.APPLICATION_JSON).body("{}").exchange()
                .expectBody(String.class).isEqualTo("reference PUT /api/items/3 -");
    }

    @Test
    void services_learnTheHostTheClientCalled() {
        client.get().uri("/api/orders/forwarded").header(HttpHeaders.HOST, "orders.example.com").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("orders.example.com");
    }

    @Test
    void health_hasOneComponentPerService() {
        client.get().uri("/actuator/health").exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.status").isEqualTo("DOWN")
                .jsonPath("$.components.commandService.status").isEqualTo("UP")
                .jsonPath("$.components.queryService.status").isEqualTo("UP")
                .jsonPath("$.components.referenceService.status").isEqualTo("DOWN")
                .jsonPath("$.components.referenceService.details").doesNotExist();
        client.get().uri("/actuator/health/readiness").exchange().expectStatus().isOk();
    }

    @Test
    void info_hasTheBuildVersion() {
        client.get().uri("/actuator/info").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.build.version").exists();
    }

    private static HttpServer stub(String name, int readinessStatus) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                exchange.getRequestBody().readAllBytes();
                String path = exchange.getRequestURI().toString();
                if (path.startsWith("/actuator/health/readiness")) {
                    exchange.sendResponseHeaders(readinessStatus, -1);
                    exchange.close();
                    return;
                }
                if (path.endsWith("/forwarded")) {
                    byte[] host = String.valueOf(exchange.getRequestHeaders().getFirst("X-Forwarded-Host"))
                            .getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, host.length);
                    exchange.getResponseBody().write(host);
                    exchange.close();
                    return;
                }
                String authorization = exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                byte[] body = (name + " " + exchange.getRequestMethod() + " " + path + " "
                        + (authorization == null ? "-" : authorization)).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String url(HttpServer server) {
        return "http://localhost:" + server.getAddress().getPort();
    }
}
