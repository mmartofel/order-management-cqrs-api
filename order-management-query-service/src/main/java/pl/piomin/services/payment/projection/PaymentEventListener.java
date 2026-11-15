package pl.piomin.services.payment.projection;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.piomin.services.config.EventRecord;
import pl.piomin.services.config.EventTypes;
import pl.piomin.services.payment.domain.event.PaymentEvent;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final PaymentProjection projection;
    private final EventTypes<PaymentEvent> eventTypes;

    public PaymentEventListener(PaymentProjection projection, JsonMapper jsonMapper) {
        this.projection = projection;
        this.eventTypes = new EventTypes<>(PaymentEvent.class, jsonMapper);
    }

    @KafkaListener(id = "payment-projection", idIsGroup = false, topics = PaymentEvent.TOPIC)
    public void on(ConsumerRecord<String, String> record) {
        EventRecord event = EventRecord.of(record);
        eventTypes.read(event).ifPresentOrElse(
                e -> projection.apply(e, event),
                () -> log.atWarn().addKeyValue("eventType", event.type()).addKeyValue("offset", record.offset())
                        .log("Unknown payment event type skipped"));
    }
}
