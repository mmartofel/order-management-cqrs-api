package pl.piomin.services.order.projection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.piomin.services.config.EventRecord;
import pl.piomin.services.order.domain.event.OrderCancelled;
import pl.piomin.services.order.domain.event.OrderDelivered;
import pl.piomin.services.order.domain.event.OrderEvent;
import pl.piomin.services.order.domain.event.OrderPaid;
import pl.piomin.services.order.domain.event.OrderPlaced;
import pl.piomin.services.order.domain.event.OrderShipped;
import pl.piomin.services.order.query.OrderLineView;
import pl.piomin.services.order.query.OrderStatus;
import pl.piomin.services.order.query.OrderView;
import pl.piomin.services.order.query.OrderViewRepository;

/**
 * Keeps {@code order_view} in step with the order events. Idempotent: an event whose sequence number is not newer
 * than the row's {@code version} was applied before and is skipped, so a redelivered record changes nothing.
 */
@Component
public class OrderProjection {

    private static final Logger log = LoggerFactory.getLogger(OrderProjection.class);

    private final OrderViewRepository repository;

    public OrderProjection(OrderViewRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void apply(OrderEvent event, EventRecord record) {
        if (event instanceof OrderPlaced placed) {
            if (repository.existsById(placed.orderId())) {
                skipped(record);
                return;
            }
            repository.save(new OrderView(placed.orderId(), placed.personId(), placed.addressId(), placed.total(),
                    placed.placedAt(), placed.lines().stream()
                            .map(l -> new OrderLineView(l.itemId(), l.sku(), l.name(), l.quantity(), l.unitPrice()))
                            .toList(),
                    record.sequence()));
            return;
        }
        OrderView view = repository.findById(event.orderId())
                // the creating event has not been applied: retried, then dead-lettered
                .orElseThrow(() -> new IllegalStateException("Order " + event.orderId() + " is not projected yet"));
        if (record.sequence() <= view.getVersion()) {
            skipped(record);
            return;
        }
        switch (event) {
            case OrderPaid e -> {
                view.setPaymentId(e.paymentId());
                view.changed(OrderStatus.PAID, record.occurredAt(), record.sequence());
            }
            case OrderShipped e -> {
                view.setDeliveryId(e.deliveryId());
                view.changed(OrderStatus.SHIPPED, record.occurredAt(), record.sequence());
            }
            case OrderDelivered e -> view.changed(OrderStatus.DELIVERED, record.occurredAt(), record.sequence());
            case OrderCancelled e -> {
                view.setCancelReason(e.reason());
                view.changed(OrderStatus.CANCELLED, record.occurredAt(), record.sequence());
            }
            case OrderPlaced e -> throw new IllegalStateException("handled above");
        }
    }

    private static void skipped(EventRecord record) {
        log.atDebug().addKeyValue("eventId", record.eventId()).addKeyValue("sequence", record.sequence())
                .log("Order event already applied, skipped");
    }
}
