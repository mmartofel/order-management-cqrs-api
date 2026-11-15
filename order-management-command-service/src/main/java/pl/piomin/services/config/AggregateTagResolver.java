package pl.piomin.services.config;

import org.axonframework.eventsourcing.eventstore.TagResolver;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventstreaming.Tag;
import org.springframework.stereotype.Component;
import pl.piomin.services.delivery.domain.event.DeliveryEvent;
import pl.piomin.services.order.domain.event.OrderEvent;
import pl.piomin.services.payment.domain.event.PaymentEvent;

import java.util.Set;

/**
 * Gives every event exactly one tag, {@code <AggregateType>=<id>}, which the aggregate-based JPA storage engine stores
 * as aggregate type and identifier. Used instead of {@code @EventTag}, so the events module stays free of Axon.
 */
@Component
public class AggregateTagResolver implements TagResolver {

    @Override
    public Set<Tag> resolve(EventMessage event) {
        return switch (event.payload()) {
            case OrderEvent e -> Set.of(Tag.of(OrderEvent.AGGREGATE_TYPE, e.orderId().toString()));
            case PaymentEvent e -> Set.of(Tag.of(PaymentEvent.AGGREGATE_TYPE, e.paymentId().toString()));
            case DeliveryEvent e -> Set.of(Tag.of(DeliveryEvent.AGGREGATE_TYPE, e.deliveryId().toString()));
            default -> Set.of();
        };
    }
}
