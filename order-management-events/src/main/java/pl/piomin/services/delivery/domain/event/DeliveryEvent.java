package pl.piomin.services.delivery.domain.event;

import java.util.UUID;

/** Events of the Delivery aggregate: facts in the past tense, never changed once stored. */
public sealed interface DeliveryEvent permits DeliveryScheduled, DeliveryShipped, DeliveryCompleted, DeliveryCancelled {

    /** Kafka topic that carries the events of all deliveries, keyed by delivery id. */
    String TOPIC = "delivery-events";

    /** Tag key of the Delivery stream in the event store. */
    String AGGREGATE_TYPE = "Delivery";

    UUID deliveryId();

    UUID orderId();
}
