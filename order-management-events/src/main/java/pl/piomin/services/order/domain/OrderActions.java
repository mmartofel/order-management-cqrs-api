package pl.piomin.services.order.domain;

import java.util.List;

/**
 * The actions an order allows in each status, as the path segments of its command endpoints. The Order aggregate
 * enforces the rules; the query service lists these actions in {@code allowedActions} for the UI. A test in the command
 * service checks both agree.
 */
public final class OrderActions {

    public static final String CANCEL = "cancel";

    private OrderActions() {
    }

    public static List<String> allowed(OrderStatus status) {
        return switch (status) {
            case PLACED, PAID -> List.of(CANCEL);
            case SHIPPED, DELIVERED, CANCELLED -> List.of();
        };
    }
}
