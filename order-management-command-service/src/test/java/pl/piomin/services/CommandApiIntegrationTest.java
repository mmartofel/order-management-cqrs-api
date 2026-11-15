package pl.piomin.services;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.testcontainers.kafka.KafkaContainer;
import pl.piomin.services.api.CommandResult;
import pl.piomin.services.delivery.command.DeliveryCreateRequest;
import pl.piomin.services.order.command.CancelOrderRequest;
import pl.piomin.services.order.command.OrderCreateRequest.OrderLineRequest;
import pl.piomin.services.order.command.OrderCreateRequest;
import pl.piomin.services.order.domain.event.OrderEvent;
import pl.piomin.services.payment.command.CompletePaymentRequest;
import pl.piomin.services.payment.command.PaymentCreateRequest;
import pl.piomin.services.payment.domain.PaymentMethod;
import pl.piomin.services.payment.domain.event.PaymentEvent;
import pl.piomin.services.reference.ReferenceClient.AddressRef;
import pl.piomin.services.reference.ReferenceClient.ItemRef;
import pl.piomin.services.reference.ReferenceClient.PersonRef;
import pl.piomin.services.reference.ReferenceClient;
import pl.piomin.services.relay.KafkaEventRelay;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

/** The command endpoints against PostgreSQL and Kafka; the reference service is replaced by a mock client. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import({TestcontainersConfig.class, TestJwtConfig.class})
class CommandApiIntegrationTest {

    @Autowired
    RestTestClient client;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    KafkaContainer kafka;

    @Autowired
    Flyway flyway;

    @Autowired
    JsonMapper jsonMapper;

    @MockitoBean
    ReferenceClient referenceClient;

    @BeforeEach
    void editorAndReferenceData() {
        client = client.mutate().defaultHeader(HttpHeaders.AUTHORIZATION, TestJwtConfig.EDITOR_TOKEN).build();
        when(referenceClient.person(1L)).thenReturn(new PersonRef(1L));
        when(referenceClient.address(2L)).thenReturn(new AddressRef(2L, 1L));
        when(referenceClient.item(10L)).thenReturn(new ItemRef(10L, "SKU-10", "Mug", new BigDecimal("19.99")));
        when(referenceClient.item(99L)).thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found",
                null, null, null));
    }

    @Test
    void migrations_allAppliedAndValidatedByHibernate() {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied()).isNotEmpty();
    }

    @Test
    void createOrder_valid_returns201WithVersion0AndRelaysOrderPlacedToKafka() {
        CommandResult result = createOrder();
        assertThat(result.version()).isZero();

        ConsumerRecord<String, String> record = recordsOf(OrderEvent.TOPIC, result.id(), 1).getFirst();
        assertThat(header(record, KafkaEventRelay.EVENT_TYPE)).isEqualTo("OrderPlaced");
        assertThat(header(record, KafkaEventRelay.SEQUENCE_NUMBER)).isEqualTo("0");
        assertThat(header(record, KafkaEventRelay.EVENT_ID)).isNotBlank();
        JsonNode payload = jsonMapper.readTree(record.value());
        assertThat(payload.get("orderId").asString()).isEqualTo(result.id().toString());
        assertThat(payload.get("total").decimalValue()).isEqualByComparingTo("39.98");
        assertThat(payload.get("lines").get(0).get("name").asString()).isEqualTo("Mug");
    }

    @Test
    void completePayment_returnsTheVersionTheRelayedEventCarries() {
        UUID orderId = createOrder().id();
        CommandResult payment = client.post().uri("/api/payments").contentType(MediaType.APPLICATION_JSON)
                .body(new PaymentCreateRequest(orderId, new BigDecimal("39.98"), PaymentMethod.CARD)).exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody(CommandResult.class).returnResult().getResponseBody();
        assertThat(payment.version()).isZero();

        client.post().uri("/api/payments/{id}/complete", payment.id()).contentType(MediaType.APPLICATION_JSON)
                .body(new CompletePaymentRequest("TX-1", 0L)).exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.id").isEqualTo(payment.id().toString())
                .jsonPath("$.version").isEqualTo(1);

        List<ConsumerRecord<String, String>> orderEvents = recordsOf(OrderEvent.TOPIC, orderId, 2);
        assertThat(orderEvents).extracting(r -> header(r, KafkaEventRelay.EVENT_TYPE))
                .containsExactly("OrderPlaced", "OrderPaid");
        List<ConsumerRecord<String, String>> paymentEvents = recordsOf(PaymentEvent.TOPIC, payment.id(), 2);
        assertThat(paymentEvents).extracting(r -> header(r, KafkaEventRelay.EVENT_TYPE))
                .containsExactly("PaymentInitiated", "PaymentCompleted");
        assertThat(header(paymentEvents.getLast(), KafkaEventRelay.SEQUENCE_NUMBER)).isEqualTo("1");
    }

    @Test
    void createOrder_asViewer_returns403AndStoresNothing() {
        long before = storedEvents();
        client.mutate().defaultHeader(HttpHeaders.AUTHORIZATION, TestJwtConfig.VIEWER_TOKEN).build()
                .post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .body(new OrderCreateRequest(1L, 2L, List.of(new OrderLineRequest(10L, 2)))).exchange()
                .expectStatus().isForbidden();
        assertThat(storedEvents()).isEqualTo(before);
    }

    @Test
    void createOrder_emptyLines_returns400AndStoresNothing() {
        long before = storedEvents();
        client.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .body(new OrderCreateRequest(1L, 2L, List.of())).exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.errors[0].field").isEqualTo("lines");
        assertThat(storedEvents()).isEqualTo(before);
    }

    @Test
    void createOrder_unknownItem_returns400NotFoundOnTheLineAndStoresNothing() {
        long before = storedEvents();
        client.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .body(new OrderCreateRequest(1L, 2L,
                        List.of(new OrderLineRequest(10L, 1), new OrderLineRequest(99L, 1)))).exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0].field").isEqualTo("lines[1].itemId")
                .jsonPath("$.errors[0].code").isEqualTo("not-found");
        assertThat(storedEvents()).isEqualTo(before);
    }

    @Test
    void cancelOrder_unknownOrder_returns404() {
        client.post().uri("/api/orders/{id}/cancel", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                .body(new CancelOrderRequest("no such order", 0L)).exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.status").isEqualTo(404);
    }

    @Test
    void cancelOrder_staleVersion_returns409WithoutErrors() {
        UUID orderId = createOrder().id();
        client.post().uri("/api/orders/{id}/cancel", orderId).contentType(MediaType.APPLICATION_JSON)
                .body(new CancelOrderRequest("changed my mind", 3L)).exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.errors").doesNotExist();
    }

    @Test
    void cancelOrder_twice_returns202Then409InvalidState() {
        UUID orderId = createOrder().id();
        client.post().uri("/api/orders/{id}/cancel", orderId).contentType(MediaType.APPLICATION_JSON)
                .body(new CancelOrderRequest("changed my mind", 0L)).exchange()
                .expectStatus().isAccepted()
                .expectBody().jsonPath("$.version").isEqualTo(1);
        client.post().uri("/api/orders/{id}/cancel", orderId).contentType(MediaType.APPLICATION_JSON)
                .body(new CancelOrderRequest("again", 1L)).exchange()
                .expectStatus().isEqualTo(409)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.errors[0].code").isEqualTo("invalid-state")
                .jsonPath("$.errors[0].field").doesNotExist();
    }

    @Test
    void createDelivery_unpaidOrder_returns409InvalidState() {
        UUID orderId = createOrder().id();
        client.post().uri("/api/deliveries").contentType(MediaType.APPLICATION_JSON)
                .body(new DeliveryCreateRequest(orderId, "DHL", LocalDate.now().plusDays(1))).exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.errors[0].code").isEqualTo("invalid-state");
    }

    @Test
    void createPayment_unknownOrder_returns400NotFoundOnOrderId() {
        client.post().uri("/api/payments").contentType(MediaType.APPLICATION_JSON)
                .body(new PaymentCreateRequest(UUID.randomUUID(), new BigDecimal("10.00"), PaymentMethod.CARD))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0].field").isEqualTo("orderId")
                .jsonPath("$.errors[0].code").isEqualTo("not-found");
    }

    @Test
    void createPayment_unknownMethod_returns400InvalidOnMethod() {
        client.post().uri("/api/payments").contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"orderId":"%s","amount":10.00,"method":"BITCOIN"}""".formatted(UUID.randomUUID()))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0].field").isEqualTo("method")
                .jsonPath("$.errors[0].code").isEqualTo("invalid");
    }

    /**
     * Writes this service's part of the contract to {@code target/openapi.json}, first writing and then asserting, so a
     * failing assertion still leaves the file to inspect. The gateway merges it into {@code contract/openapi.json}.
     */
    @Test
    void exportOpenApiContract() throws Exception {
        String json = client.get().uri("/v3/api-docs").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        Path file = Path.of("target/openapi.json");
        Files.writeString(file, json + System.lineSeparator());

        DocumentContext contract = JsonPath.parse(json);
        assertThat(contract.read("$.paths['/api/orders'].post.operationId", String.class)).isEqualTo("createOrder");
        assertThat(contract.read("$.paths['/api/orders'].post.responses", Map.class)).containsKeys("201", "400", "409");
        assertThat(contract.read("$.paths['/api/orders/{id}/cancel'].post.operationId", String.class))
                .isEqualTo("cancelOrder");
        assertThat(contract.read("$.paths['/api/orders/{id}/cancel'].post.responses", Map.class))
                .containsKeys("202", "400", "404", "409");
        assertThat(contract.read("$.paths['/api/deliveries/{id}/ship'].post.operationId", String.class))
                .isEqualTo("shipDelivery");
        assertThat(contract.read("$.paths[*][?(@.put || @.delete)]", List.class)).isEmpty();
        assertThat(contract.read("$.components.schemas.CommandResult.required", List.class)).contains("id", "version");
        assertThat(contract.read("$.components.schemas.CancelOrderRequest.required", List.class))
                .contains("reason", "version");
        assertThat(contract.read("$.components.schemas.FieldError.properties.code.enum", List.class))
                .contains("not-found", "invalid-state");
        assertThat(contract.read("$.components.schemas.PaymentCreateRequest.properties.method.enum", List.class))
                .containsExactly("CARD", "TRANSFER", "BLIK", "CASH_ON_DELIVERY");
    }

    private CommandResult createOrder() {
        return client.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .body(new OrderCreateRequest(1L, 2L, List.of(new OrderLineRequest(10L, 2)))).exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody(CommandResult.class).returnResult().getResponseBody();
    }

    private long storedEvents() {
        return jdbc.sql("SELECT count(*) FROM aggregate_event_entry").query(Long.class).single();
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }

    /** Reads the topic from the beginning until it holds {@code expected} records keyed by the aggregate id. */
    private List<ConsumerRecord<String, String>> recordsOf(String topic, UUID aggregateId, int expected) {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        List<ConsumerRecord<String, String>> found = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config, new StringDeserializer(),
                new StringDeserializer())) {
            consumer.subscribe(List.of(topic));
            await().atMost(Duration.ofSeconds(30)).until(() -> {
                consumer.poll(Duration.ofMillis(500)).forEach(record -> {
                    if (aggregateId.toString().equals(record.key())) {
                        found.add(record);
                    }
                });
                return found.size() >= expected;
            });
        }
        return found;
    }
}
