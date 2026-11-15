package pl.piomin.services;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pl.piomin.services.delivery.domain.Delivery;
import pl.piomin.services.delivery.domain.DeliveryActions;
import pl.piomin.services.delivery.domain.DeliveryStates;
import pl.piomin.services.delivery.domain.DeliveryStatus;
import pl.piomin.services.exception.BusinessRuleViolationException;
import pl.piomin.services.order.domain.Order;
import pl.piomin.services.order.domain.OrderActions;
import pl.piomin.services.order.domain.OrderStates;
import pl.piomin.services.order.domain.OrderStatus;
import pl.piomin.services.payment.domain.Payment;
import pl.piomin.services.payment.domain.PaymentActions;
import pl.piomin.services.payment.domain.PaymentStates;
import pl.piomin.services.payment.domain.PaymentStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The query service lists {@code allowedActions} from the rules in the events module ({@code OrderActions},
 * {@code PaymentActions}, {@code DeliveryActions}). This test runs every action against the aggregates in every
 * status, with the same domain calls the command handlers make, and asserts the aggregates accept exactly the actions
 * those rules list.
 */
class ActionsAgreementTest {

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    void order_acceptsExactlyTheListedActions(OrderStatus status) {
        List<String> accepted = new ArrayList<>();
        if (accepts(() -> OrderStates.in(status).cancel("test"))) {
            accepted.add(OrderActions.CANCEL);
        }
        assertThat(accepted).containsExactlyInAnyOrderElementsOf(OrderActions.allowed(status));
    }

    @ParameterizedTest
    @EnumSource(PaymentStatus.class)
    void payment_acceptsExactlyTheListedActions(PaymentStatus status) {
        for (OrderStatus orderStatus : OrderStatus.values()) {
            List<String> accepted = new ArrayList<>();
            if (accepts(() -> {
                PaymentStates.in(status).complete("TX-2");
                OrderStates.in(orderStatus).pay(UUID.randomUUID());
            })) {
                accepted.add(PaymentActions.COMPLETE);
            }
            if (accepts(() -> PaymentStates.in(status).fail("declined"))) {
                accepted.add(PaymentActions.FAIL);
            }
            if (accepts(() -> {
                PaymentStates.in(status).refund("cancelled");
                OrderStates.in(orderStatus).acceptsRefund();
            })) {
                accepted.add(PaymentActions.REFUND);
            }
            assertThat(accepted).as("payment %s, order %s", status, orderStatus)
                    .containsExactlyInAnyOrderElementsOf(PaymentActions.allowed(status, orderStatus));
        }
    }

    @ParameterizedTest
    @EnumSource(DeliveryStatus.class)
    void delivery_acceptsExactlyTheListedActions(DeliveryStatus status) {
        for (OrderStatus orderStatus : OrderStatus.values()) {
            List<String> accepted = new ArrayList<>();
            if (accepts(() -> {
                DeliveryStates.in(status).ship("TRACK-2");
                OrderStates.in(orderStatus).ship(UUID.randomUUID());
            })) {
                accepted.add(DeliveryActions.SHIP);
            }
            if (accepts(() -> {
                DeliveryStates.in(status).complete();
                OrderStates.in(orderStatus).deliver(UUID.randomUUID());
            })) {
                accepted.add(DeliveryActions.COMPLETE);
            }
            if (accepts(() -> DeliveryStates.in(status).cancel("test"))) {
                accepted.add(DeliveryActions.CANCEL);
            }
            assertThat(accepted).as("delivery %s, order %s", status, orderStatus)
                    .containsExactlyInAnyOrderElementsOf(DeliveryActions.allowed(status, orderStatus));
        }
    }

    private static boolean accepts(Runnable command) {
        try {
            command.run();
            return true;
        } catch (BusinessRuleViolationException e) {
            return false;
        }
    }
}
