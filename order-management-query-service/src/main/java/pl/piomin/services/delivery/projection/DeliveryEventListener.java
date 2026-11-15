package pl.piomin.services.delivery.projection;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.piomin.services.config.EventRecord;
import pl.piomin.services.config.EventTypes;
import pl.piomin.services.delivery.domain.event.DeliveryEvent;
import tools.jackson.databind.json.JsonMapper;

@Component
public class DeliveryEventListener {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventListener.class);

    private final DeliveryProjection projection;
    private final EventTypes<DeliveryEvent> eventTypes;

    public DeliveryEventListener(DeliveryProjection projection, JsonMapper jsonMapper) {
        this.projection = projection;
        this.eventTypes = new EventTypes<>(DeliveryEvent.class, jsonMapper);
    }

    @KafkaListener(id = "delivery-projection", idIsGroup = false, topics = DeliveryEvent.TOPIC)
    public void on(ConsumerRecord<String, String> record) {
        EventRecord event = EventRecord.of(record);
        eventTypes.read(event).ifPresentOrElse(
                e -> projection.apply(e, event),
                () -> log.atWarn().addKeyValue("eventType", event.type()).addKeyValue("offset", record.offset())
                        .log("Unknown delivery event type skipped"));
    }
}
