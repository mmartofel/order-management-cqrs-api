package pl.piomin.services.order.domain.event;

import java.util.UUID;

/** Events of the Order aggregate: facts in the past tense, never changed once stored. */
public sealed interface OrderEvent permits OrderPlaced, OrderPaid, OrderShipped, OrderDelivered, OrderCancelled {

    /** Kafka topic that carries the events of all orders, keyed by order id. */
    String TOPIC = "order-events";

    /** Tag key of the Order stream in the event store. */
    String AGGREGATE_TYPE = "Order";

    UUID orderId();
}
