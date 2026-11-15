package pl.piomin.services.payment.domain;

import pl.piomin.services.order.domain.OrderStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * The actions a payment allows, as the path segments of its command endpoints. Two of them also depend on the order:
 * completing a payment marks its order paid (so the order must be PLACED), and only the payment of a cancelled order
 * is refunded. The command service enforces the rules; a test there checks they agree with this class.
 */
public final class PaymentActions {

    public static final String COMPLETE = "complete";
    public static final String FAIL = "fail";
    public static final String REFUND = "refund";

    private PaymentActions() {
    }

    public static List<String> allowed(PaymentStatus status, OrderStatus orderStatus) {
        List<String> actions = new ArrayList<>();
        switch (status) {
            case INITIATED -> {
                if (orderStatus == OrderStatus.PLACED) {
                    actions.add(COMPLETE);
                }
                actions.add(FAIL);
            }
            case COMPLETED -> {
                if (orderStatus == OrderStatus.CANCELLED) {
                    actions.add(REFUND);
                }
            }
            case FAILED, REFUNDED -> {
            }
        }
        return List.copyOf(actions);
    }
}
