package pl.piomin.services.order.projection;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.piomin.services.config.EventRecord;
import pl.piomin.services.config.EventTypes;
import pl.piomin.services.order.domain.event.OrderEvent;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final OrderProjection projection;
    private final EventTypes<OrderEvent> eventTypes;

    public OrderEventListener(OrderProjection projection, JsonMapper jsonMapper) {
        this.projection = projection;
        this.eventTypes = new EventTypes<>(OrderEvent.class, jsonMapper);
    }

    @KafkaListener(id = "order-projection", idIsGroup = false, topics = OrderEvent.TOPIC)
    public void on(ConsumerRecord<String, String> record) {
        EventRecord event = EventRecord.of(record);
        eventTypes.read(event).ifPresentOrElse(
                e -> projection.apply(e, event),
                () -> log.atWarn().addKeyValue("eventType", event.type()).addKeyValue("offset", record.offset())
                        .log("Unknown order event type skipped"));
    }
}
