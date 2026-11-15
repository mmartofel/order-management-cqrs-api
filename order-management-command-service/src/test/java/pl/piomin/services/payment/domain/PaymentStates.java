package pl.piomin.services.payment.domain;

import pl.piomin.services.payment.domain.event.PaymentCompleted;
import pl.piomin.services.payment.domain.event.PaymentFailed;
import pl.piomin.services.payment.domain.event.PaymentInitiated;
import pl.piomin.services.payment.domain.event.PaymentRefunded;

import java.math.BigDecimal;
import java.util.UUID;

/** Builds a payment in a given status the way Axon does: by applying its events. */
public final class PaymentStates {

    private PaymentStates() {
    }

    public static Payment in(PaymentStatus status) {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Payment payment = new Payment();
        payment.on(new PaymentInitiated(id, orderId, new BigDecimal("39.98"), "CARD"));
        switch (status) {
            case INITIATED -> {
            }
            case COMPLETED -> payment.on(new PaymentCompleted(id, orderId, "TX-1"));
            case FAILED -> payment.on(new PaymentFailed(id, orderId, "declined"));
            case REFUNDED -> {
                payment.on(new PaymentCompleted(id, orderId, "TX-1"));
                payment.on(new PaymentRefunded(id, orderId, "cancelled"));
            }
        }
        return payment;
    }
}
