package pl.piomin.services.order.domain;

import org.axonframework.eventsourcing.annotation.EventSourcingHandler;
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator;
import org.axonframework.extension.spring.stereotype.EventSourced;
import pl.piomin.services.exception.BusinessRuleViolationException;
import pl.piomin.services.order.domain.event.OrderCancelled;
import pl.piomin.services.order.domain.event.OrderDelivered;
import pl.piomin.services.order.domain.event.OrderEvent;
import pl.piomin.services.order.domain.event.OrderPaid;
import pl.piomin.services.order.domain.event.OrderPlaced;
import pl.piomin.services.order.domain.event.OrderShipped;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event-sourced Order. Decision methods check a rule against the current state and return the event to append; the
 * event-sourcing handlers only assign state, because they also run when the order is loaded from its events.
 */
@EventSourced(tagKey = OrderEvent.AGGREGATE_TYPE, idType = UUID.class)
public class Order {

    private UUID id;
    private Long addressId;
    private BigDecimal total;
    private OrderStatus status;

    @EntityCreator
    public Order() {
    }

    public OrderPaid pay(UUID paymentId) {
        requireStatus(OrderStatus.PLACED, "paid");
        return new OrderPaid(id, paymentId);
    }

    public OrderShipped ship(UUID deliveryId) {
        requireStatus(OrderStatus.PAID, "shipped");
        return new OrderShipped(id, deliveryId);
    }

    public OrderDelivered deliver(UUID deliveryId) {
        requireStatus(OrderStatus.SHIPPED, "delivered");
        return new OrderDelivered(id, deliveryId);
    }

    public OrderCancelled cancel(String reason) {
        if (status != OrderStatus.PLACED && status != OrderStatus.PAID) {
            throw new BusinessRuleViolationException("Order " + id + " is " + status + " and cannot be cancelled");
        }
        return new OrderCancelled(id, reason);
    }

    /** A payment must cover the whole order, and only an order that is not paid or cancelled can take one. */
    public void acceptsPayment(BigDecimal amount) {
        requireStatus(OrderStatus.PLACED, "paid");
        if (total.compareTo(amount) != 0) {
            throw new BusinessRuleViolationException(
                    "Payment amount " + amount + " does not match order total " + total);
        }
    }

    public void acceptsDelivery() {
        requireStatus(OrderStatus.PAID, "scheduled for delivery");
    }

    private void requireStatus(OrderStatus expected, String action) {
        if (status != expected) {
            throw new BusinessRuleViolationException(
                    "Order " + id + " is " + status + " and cannot be " + action + "; expected " + expected);
        }
    }

    @EventSourcingHandler
    void on(OrderPlaced event) {
        this.id = event.orderId();
        this.addressId = event.addressId();
        this.total = event.total();
        this.status = OrderStatus.PLACED;
    }

    @EventSourcingHandler
    void on(OrderPaid event) {
        this.status = OrderStatus.PAID;
    }

    @EventSourcingHandler
    void on(OrderShipped event) {
        this.status = OrderStatus.SHIPPED;
    }

    @EventSourcingHandler
    void on(OrderDelivered event) {
        this.status = OrderStatus.DELIVERED;
    }

    @EventSourcingHandler
    void on(OrderCancelled event) {
        this.status = OrderStatus.CANCELLED;
    }

    public UUID id() {
        return id;
    }

    public Long addressId() {
        return addressId;
    }

    public BigDecimal total() {
        return total;
    }

    public OrderStatus status() {
        return status;
    }
}
