package pl.piomin.services;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.testcontainers.kafka.KafkaContainer;
import pl.piomin.services.api.CommandResult;
import pl.piomin.services.order.command.CancelOrderRequest;
import pl.piomin.services.order.command.PlaceOrderRequest;
import pl.piomin.services.order.domain.event.OrderEvent;
import pl.piomin.services.payment.command.CompletePaymentRequest;
import pl.piomin.services.payment.command.InitiatePaymentRequest;
import pl.piomin.services.delivery.command.ScheduleDeliveryRequest;
import pl.piomin.services.payment.domain.event.PaymentEvent;
import pl.piomin.services.reference.ReferenceClient;
import pl.piomin.services.reference.ReferenceClient.AddressRef;
import pl.piomin.services.reference.ReferenceClient.ItemRef;
import pl.piomin.services.reference.ReferenceClient.PersonRef;
import pl.piomin.services.relay.KafkaEventRelay;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
@Import(TestcontainersConfig.class)
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
    void referenceData() {
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
    void placeOrder_valid_returns201AndRelaysOrderPlacedToKafka() {
        UUID orderId = placeOrder();

        ConsumerRecord<String, String> record = recordsOf(OrderEvent.TOPIC, orderId, 1).getFirst();
        assertThat(header(record, KafkaEventRelay.EVENT_TYPE)).isEqualTo("OrderPlaced");
        assertThat(header(record, KafkaEventRelay.SEQUENCE_NUMBER)).isEqualTo("0");
        assertThat(header(record, KafkaEventRelay.EVENT_ID)).isNotBlank();
        JsonNode payload = jsonMapper.readTree(record.value());
        assertThat(payload.get("orderId").asString()).isEqualTo(orderId.toString());
        assertThat(payload.get("total").decimalValue()).isEqualByComparingTo("39.98");
        assertThat(payload.get("lines").get(0).get("name").asString()).isEqualTo("Mug");
    }

    @Test
    void payOrder_completePayment_appendsPaymentCompletedAndOrderPaidInOrder() {
        UUID orderId = placeOrder();
        UUID paymentId = client.post().uri("/api/v1/payments").contentType(MediaType.APPLICATION_JSON)
                .body(new InitiatePaymentRequest(orderId, new BigDecimal("39.98"), "CARD")).exchange()
                .expectStatus().isCreated()
                .expectBody(CommandResult.class).returnResult().getResponseBody().id();

        client.post().uri("/api/v1/payments/{id}/complete", paymentId).contentType(MediaType.APPLICATION_JSON)
                .body(new CompletePaymentRequest("TX-1")).exchange()
                .expectStatus().isAccepted()
                .expectBody().jsonPath("$.id").isEqualTo(paymentId.toString());

        List<ConsumerRecord<String, String>> orderEvents = recordsOf(OrderEvent.TOPIC, orderId, 2);
        assertThat(orderEvents).extracting(r -> header(r, KafkaEventRelay.EVENT_TYPE))
                .containsExactly("OrderPlaced", "OrderPaid");
        assertThat(recordsOf(PaymentEvent.TOPIC, paymentId, 2)).extracting(r -> header(r, KafkaEventRelay.EVENT_TYPE))
                .containsExactly("PaymentInitiated", "PaymentCompleted");
    }

    @Test
    void placeOrder_emptyLines_returns400AndStoresNothing() {
        long before = storedEvents();
        client.post().uri("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                .body(new PlaceOrderRequest(1L, 2L, List.of())).exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.errors[0].field").isEqualTo("lines");
        assertThat(storedEvents()).isEqualTo(before);
    }

    @Test
    void placeOrder_unknownItem_returns422AndStoresNothing() {
        long before = storedEvents();
        client.post().uri("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                .body(new PlaceOrderRequest(1L, 2L, List.of(new PlaceOrderRequest.Line(99L, 1)))).exchange()
                .expectStatus().isEqualTo(422)
                .expectBody().jsonPath("$.detail").isEqualTo("Item 99 does not exist");
        assertThat(storedEvents()).isEqualTo(before);
    }

    @Test
    void cancelOrder_unknownOrder_returns404() {
        client.post().uri("/api/v1/orders/{id}/cancel", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                .body(new CancelOrderRequest("no such order")).exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.status").isEqualTo(404);
    }

    @Test
    void cancelOrder_twice_returns202Then409() {
        UUID orderId = placeOrder();
        client.post().uri("/api/v1/orders/{id}/cancel", orderId).contentType(MediaType.APPLICATION_JSON)
                .body(new CancelOrderRequest("changed my mind")).exchange()
                .expectStatus().isAccepted();
        client.post().uri("/api/v1/orders/{id}/cancel", orderId).contentType(MediaType.APPLICATION_JSON)
                .body(new CancelOrderRequest("again")).exchange()
                .expectStatus().isEqualTo(409)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void scheduleDelivery_unpaidOrder_returns409() {
        UUID orderId = placeOrder();
        client.post().uri("/api/v1/deliveries").contentType(MediaType.APPLICATION_JSON)
                .body(new ScheduleDeliveryRequest(orderId, "DHL", LocalDate.now().plusDays(1))).exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void initiatePayment_unknownOrder_returns422() {
        client.post().uri("/api/v1/payments").contentType(MediaType.APPLICATION_JSON)
                .body(new InitiatePaymentRequest(UUID.randomUUID(), new BigDecimal("10.00"), "CARD")).exchange()
                .expectStatus().isEqualTo(422);
    }

    @Test
    void initiatePayment_unsupportedMethod_returns400() {
        client.post().uri("/api/v1/payments").contentType(MediaType.APPLICATION_JSON)
                .body(new InitiatePaymentRequest(UUID.randomUUID(), new BigDecimal("10.00"), "BITCOIN")).exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors[0].field").isEqualTo("method");
    }

    private UUID placeOrder() {
        CommandResult result = client.post().uri("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                .body(new PlaceOrderRequest(1L, 2L, List.of(new PlaceOrderRequest.Line(10L, 2)))).exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody(CommandResult.class).returnResult().getResponseBody();
        return result.id();
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
