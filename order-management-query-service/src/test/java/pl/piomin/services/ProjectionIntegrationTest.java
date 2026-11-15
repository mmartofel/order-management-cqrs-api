package pl.piomin.services;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.utils.Utils;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.kafka.KafkaContainer;
import pl.piomin.services.config.EventRecord;
import pl.piomin.services.delivery.domain.event.DeliveryEvent;
import pl.piomin.services.delivery.domain.event.DeliveryScheduled;
import pl.piomin.services.delivery.domain.event.DeliveryShipped;
import pl.piomin.services.order.domain.event.OrderEvent;
import pl.piomin.services.order.domain.event.OrderLine;
import pl.piomin.services.order.domain.event.OrderPaid;
import pl.piomin.services.order.domain.event.OrderPlaced;
import pl.piomin.services.order.domain.event.OrderShipped;
import pl.piomin.services.order.query.OrderResponse;
import pl.piomin.services.order.query.OrderStatus;
import pl.piomin.services.payment.domain.event.PaymentCompleted;
import pl.piomin.services.payment.domain.event.PaymentEvent;
import pl.piomin.services.payment.domain.event.PaymentInitiated;
import pl.piomin.services.payment.query.PaymentStatus;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Publishes event records the way the command service's relay does (key = aggregate id, headers event-type, event-id,
 * sequence-number, occurred-at) and asserts the read models and the query endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfig.class)
class ProjectionIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-11-15T10:00:00Z");

    @Autowired
    RestTestClient client;

    @Autowired
    KafkaTemplate<String, String> kafka;

    @Autowired
    KafkaContainer kafkaContainer;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    Flyway flyway;

    @Test
    void migrations_allApplied() {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied()).isNotEmpty();
    }

    @Test
    void orderLifecycle_eventsProjected_queryReturnsOrderWithLinesPaymentAndDelivery() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        publishOrder(placed(orderId, 7L), 0);
        publishPayment(new PaymentInitiated(paymentId, orderId, new BigDecimal("39.98"), "CARD"), 0);
        publishPayment(new PaymentCompleted(paymentId, orderId, "TX-1"), 1);
        publishOrder(new OrderPaid(orderId, paymentId), 1);
        publishDelivery(new DeliveryScheduled(deliveryId, orderId, 2L, "DHL", LocalDate.of(2026, 11, 20)), 0);
        publishDelivery(new DeliveryShipped(deliveryId, orderId, "TRACK-1"), 1);
        publishOrder(new OrderShipped(orderId, deliveryId), 2);

        OrderResponse order = awaitOrder(orderId, o -> o.status() == OrderStatus.SHIPPED
                && !o.payments().isEmpty() && o.payments().getFirst().status() == PaymentStatus.COMPLETED
                && !o.deliveries().isEmpty() && "TRACK-1".equals(o.deliveries().getFirst().trackingNumber()));

        assertThat(order.total()).isEqualByComparingTo("39.98");
        assertThat(order.lines()).singleElement()
                .satisfies(line -> {
                    assertThat(line.name()).isEqualTo("Mug");
                    assertThat(line.quantity()).isEqualTo(2);
                });
        assertThat(order.version()).isEqualTo(2);

        client.get().uri("/api/v1/payments/{id}", paymentId).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.transactionReference").isEqualTo("TX-1");
        client.get().uri("/api/v1/deliveries?orderId={id}", orderId).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.content[0].status").isEqualTo("SHIPPED");
    }

    @Test
    void redeliveredAndOlderEvents_areSkipped() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        publishOrder(placed(orderId, 7L), 0);
        publishOrder(new OrderPaid(orderId, paymentId), 1);
        publishOrder(new OrderShipped(orderId, deliveryId), 2);
        awaitOrder(orderId, o -> o.status() == OrderStatus.SHIPPED);

        // the same events again, as after a relay retry
        publishOrder(placed(orderId, 7L), 0);
        publishOrder(new OrderPaid(orderId, paymentId), 1);
        // a new order on the same partition: once it is visible, the duplicates before it were consumed
        UUID marker = keyOnSamePartition(OrderEvent.TOPIC, orderId);
        publishOrder(placed(marker, 7L), 0);
        awaitOrder(marker, o -> true);

        OrderResponse order = awaitOrder(orderId, o -> true);
        assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.version()).isEqualTo(2);
        assertThat(order.lines()).hasSize(1);
    }

    @Test
    void unknownEventType_isSkippedAndLaterEventsAreApplied() {
        UUID orderId = UUID.randomUUID();
        publishOrder(placed(orderId, 7L), 0);
        send(OrderEvent.TOPIC, orderId, "OrderGiftWrapped", "{\"orderId\":\"" + orderId + "\"}", 1);
        publishOrder(new OrderPaid(orderId, UUID.randomUUID()), 2);

        assertThat(awaitOrder(orderId, o -> o.status() == OrderStatus.PAID).version()).isEqualTo(2);
    }

    @Test
    void payloadWithFieldUnknownToThisVersion_isStillApplied() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        publishOrder(placed(orderId, 7L), 0);
        // a newer command service added a field to OrderPaid; this query service does not know it yet
        send(OrderEvent.TOPIC, orderId, "OrderPaid",
                "{\"orderId\":\"" + orderId + "\",\"paymentId\":\"" + paymentId + "\",\"paidVia\":\"CARD\"}", 1);

        assertThat(awaitOrder(orderId, o -> o.status() == OrderStatus.PAID).status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void recordWithoutHeaders_goesToDeadLetterTopic() {
        UUID orderId = UUID.randomUUID();
        kafka.send(new ProducerRecord<>(OrderEvent.TOPIC, orderId.toString(), "{\"broken\":true}"));

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"), new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(OrderEvent.TOPIC + "-dlt"));
            await().atMost(Duration.ofSeconds(30)).until(() -> {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (orderId.toString().equals(record.key())) {
                        return true;
                    }
                }
                return false;
            });
        }
    }

    @Test
    void listOrders_filteredByPersonAndStatus_returnsPagedModel() {
        long personId = Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000) + 1_000;
        UUID placedOrder = UUID.randomUUID();
        UUID paidOrder = UUID.randomUUID();
        publishOrder(placed(placedOrder, personId), 0);
        publishOrder(placed(paidOrder, personId), 0);
        publishOrder(new OrderPaid(paidOrder, UUID.randomUUID()), 1);
        awaitOrder(paidOrder, o -> o.status() == OrderStatus.PAID);
        awaitOrder(placedOrder, o -> true);

        client.get().uri("/api/v1/orders?personId={p}&status=PAID&size=10", personId).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.page.totalElements").isEqualTo(1)
                .jsonPath("$.content[0].id").isEqualTo(paidOrder.toString());
    }

    @Test
    void getOrder_unknownId_returns404ProblemDetail() {
        client.get().uri("/api/v1/orders/{id}", UUID.randomUUID()).exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.status").isEqualTo(404);
    }

    @Test
    void getOrder_malformedId_returns400() {
        client.get().uri("/api/v1/orders/not-a-uuid").exchange().expectStatus().isBadRequest();
    }

    @Test
    void listOrders_unknownStatusOrSort_returns400() {
        client.get().uri("/api/v1/orders?status=LOST").exchange().expectStatus().isBadRequest();
        client.get().uri("/api/v1/orders?sort=nope").exchange().expectStatus().isBadRequest();
    }

    private OrderResponse awaitOrder(UUID orderId, java.util.function.Predicate<OrderResponse> condition) {
        OrderResponse[] found = new OrderResponse[1];
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200)).until(() -> {
            var result = client.get().uri("/api/v1/orders/{id}", orderId).exchange().returnResult(String.class);
            if (result.getStatus().value() != 200) {
                return false;
            }
            found[0] = jsonMapper.readValue(result.getResponseBody(), OrderResponse.class);
            return condition.test(found[0]);
        });
        return found[0];
    }

    /** A random id that Kafka's default partitioner puts on the same partition as {@code key}. */
    private UUID keyOnSamePartition(String topic, UUID key) {
        int partitions = kafka.partitionsFor(topic).size();
        int target = partition(key, partitions);
        UUID candidate;
        do {
            candidate = UUID.randomUUID();
        } while (partition(candidate, partitions) != target);
        return candidate;
    }

    private static int partition(UUID key, int partitions) {
        return Utils.toPositive(Utils.murmur2(key.toString().getBytes(StandardCharsets.UTF_8))) % partitions;
    }

    private static OrderPlaced placed(UUID orderId, long personId) {
        return new OrderPlaced(orderId, personId, 2L,
                List.of(new OrderLine(10L, "SKU-10", "Mug", 2, new BigDecimal("19.99"))), new BigDecimal("39.98"), NOW);
    }

    private void publishOrder(OrderEvent event, long sequence) {
        send(OrderEvent.TOPIC, event.orderId(), event.getClass().getSimpleName(), jsonMapper.writeValueAsString(event),
                sequence);
    }

    private void publishPayment(PaymentEvent event, long sequence) {
        send(PaymentEvent.TOPIC, event.paymentId(), event.getClass().getSimpleName(),
                jsonMapper.writeValueAsString(event), sequence);
    }

    private void publishDelivery(DeliveryEvent event, long sequence) {
        send(DeliveryEvent.TOPIC, event.deliveryId(), event.getClass().getSimpleName(),
                jsonMapper.writeValueAsString(event), sequence);
    }

    private void send(String topic, UUID key, String type, String payload, long sequence) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key.toString(), payload);
        header(record, EventRecord.EVENT_TYPE, type);
        header(record, EventRecord.EVENT_ID, UUID.randomUUID().toString());
        header(record, EventRecord.SEQUENCE_NUMBER, String.valueOf(sequence));
        header(record, EventRecord.OCCURRED_AT, NOW.plusSeconds(sequence).toString());
        kafka.send(record).join();
    }

    private static void header(ProducerRecord<String, String> record, String name, String value) {
        record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
    }
}
