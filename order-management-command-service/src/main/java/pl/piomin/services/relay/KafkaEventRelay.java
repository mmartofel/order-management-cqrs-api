package pl.piomin.services.relay;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.axonframework.messaging.eventhandling.annotation.SequenceNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pl.piomin.services.delivery.domain.event.DeliveryCancelled;
import pl.piomin.services.delivery.domain.event.DeliveryCompleted;
import pl.piomin.services.delivery.domain.event.DeliveryEvent;
import pl.piomin.services.delivery.domain.event.DeliveryScheduled;
import pl.piomin.services.delivery.domain.event.DeliveryShipped;
import pl.piomin.services.order.domain.event.OrderCancelled;
import pl.piomin.services.order.domain.event.OrderDelivered;
import pl.piomin.services.order.domain.event.OrderEvent;
import pl.piomin.services.order.domain.event.OrderPaid;
import pl.piomin.services.order.domain.event.OrderPlaced;
import pl.piomin.services.order.domain.event.OrderShipped;
import pl.piomin.services.payment.domain.event.PaymentCompleted;
import pl.piomin.services.payment.domain.event.PaymentEvent;
import pl.piomin.services.payment.domain.event.PaymentFailed;
import pl.piomin.services.payment.domain.event.PaymentInitiated;
import pl.piomin.services.payment.domain.event.PaymentRefunded;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * The only class that publishes to Kafka. It runs in its own pooled streaming processor ({@code kafka-relay}) that reads
 * the event store after the command's transaction committed; Axon's token store keeps its position, so an event that
 * fails to reach the broker is retried and the topic only ever holds committed events.
 * <p>
 * Record key = aggregate id, so all events of one aggregate land on one partition in order. Headers carry the event
 * type, the event id and the aggregate sequence number, which the query service uses to apply each event once.
 */
@Component
public class KafkaEventRelay {

    public static final String EVENT_TYPE = "event-type";
    public static final String EVENT_ID = "event-id";
    public static final String SEQUENCE_NUMBER = "sequence-number";
    public static final String OCCURRED_AT = "occurred-at";

    private static final Logger log = LoggerFactory.getLogger(KafkaEventRelay.class);

    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper jsonMapper;
    private final RelayProperties properties;

    public KafkaEventRelay(KafkaTemplate<String, String> kafka, JsonMapper jsonMapper, RelayProperties properties) {
        this.kafka = kafka;
        this.jsonMapper = jsonMapper;
        this.properties = properties;
    }

    // Axon subscribes an event handler by the event's type name, so every event type gets its own method.

    @EventHandler
    void on(OrderPlaced event, EventMessage message, @SequenceNumber Long sequence) throws Exception {
        order(event, message, sequence);
    }

    @EventHandler
    void on(OrderPaid event, EventMessage message, @SequenceNumber Long sequence) throws Exception {
        order(event, message, sequence);
    }

    @EventHandler
    void on(OrderShipped event, EventMessage message, @SequenceNumber Long sequence) throws Exception {
        order(event, message, sequence);
    }

    @EventHandler
    void on(OrderDelivered event, EventMessage message, @SequenceNumber Long sequence) throws Exception {
        order(event, message, sequence);
    }

    @EventHandler
    void on(OrderCancelled event, EventMessage message, @SequenceNumber Long sequence) throws Exception {
        order(event, message, sequence);
    }

    @EventHandler
    void on(PaymentInitiated event, EventMessage message, @SequenceNumber Long sequence) throws Exception {
        payment(event, message, sequence);
    }

    @EventHandler
    void on(PaymentCompleted event, EventMessage message, @SequenceNumber Long sequence) throws Exception {
        payment(event, message, sequence);
    }

    @EventHandler
    void on(PaymentFailed event, EventMessage message, @SequenceNumber Long sequence) throws Exception {
        payment(event, message, sequence);
    }

    @EventHandler
    void on(PaymentRefunded event, EventMessage message, @SequenceNumber Long sequence) throws Exception {
        payment(event, message, sequence);
    }

    @EventHandler
    void on(DeliveryScheduled event, EventMessage message, @SequenceNumber Long sequence) throws Exception {
        delivery(event, message, sequence);
    }

    @EventHandler
    void on(DeliveryShipped event, EventMessage message, @SequenceNumber Long sequence) throws Exception {
        delivery(event, message, sequence);
    }

    @EventHandler
    void on(DeliveryCompleted event, EventMessage message, @SequenceNumber Long sequence) throws Exception {
        delivery(event, message, sequence);
    }

    @EventHandler
    void on(DeliveryCancelled event, EventMessage message, @SequenceNumber Long sequence) throws Exception {
        delivery(event, message, sequence);
    }

    private void order(OrderEvent event, EventMessage message, Long sequence) throws Exception {
        send(OrderEvent.TOPIC, event.orderId(), event, message, sequence);
    }

    private void payment(PaymentEvent event, EventMessage message, Long sequence) throws Exception {
        send(PaymentEvent.TOPIC, event.paymentId(), event, message, sequence);
    }

    private void delivery(DeliveryEvent event, EventMessage message, Long sequence) throws Exception {
        send(DeliveryEvent.TOPIC, event.deliveryId(), event, message, sequence);
    }

    private void send(String topic, UUID aggregateId, Object event, EventMessage message, Long sequence)
            throws Exception {
        String type = event.getClass().getSimpleName();
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, aggregateId.toString(),
                jsonMapper.writeValueAsString(event));
        header(record, EVENT_TYPE, type);
        header(record, EVENT_ID, message.identifier());
        header(record, SEQUENCE_NUMBER, String.valueOf(sequence));
        header(record, OCCURRED_AT, message.timestamp().toString());
        // wait for the broker's acknowledgement; a failure fails the batch and the processor retries it
        kafka.send(record).get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
        log.atDebug().addKeyValue("topic", topic).addKeyValue("eventType", type)
                .addKeyValue("aggregateId", aggregateId).addKeyValue("sequence", sequence).log("Event relayed");
    }

    private static void header(ProducerRecord<String, String> record, String name, String value) {
        record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
    }
}
