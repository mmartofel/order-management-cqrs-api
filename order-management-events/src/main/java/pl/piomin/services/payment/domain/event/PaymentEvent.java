package pl.piomin.services.payment.domain.event;

import java.util.UUID;

/** Events of the Payment aggregate: facts in the past tense, never changed once stored. */
public sealed interface PaymentEvent permits PaymentInitiated, PaymentCompleted, PaymentFailed, PaymentRefunded {

    /** Kafka topic that carries the events of all payments, keyed by payment id. */
    String TOPIC = "payment-events";

    /** Tag key of the Payment stream in the event store. */
    String AGGREGATE_TYPE = "Payment";

    UUID paymentId();

    UUID orderId();
}
