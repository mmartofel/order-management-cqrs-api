package pl.piomin.services.delivery.domain;

import pl.piomin.services.order.domain.OrderStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * The actions a delivery allows, as the path segments of its command endpoints. Shipping and completing a delivery
 * also move its order on, so they depend on the order's status. The command service enforces the rules; a test there
 * checks they agree with this class.
 */
public final class DeliveryActions {

    public static final String SHIP = "ship";
    public static final String COMPLETE = "complete";
    public static final String CANCEL = "cancel";

    private DeliveryActions() {
    }

    public static List<String> allowed(DeliveryStatus status, OrderStatus orderStatus) {
        List<String> actions = new ArrayList<>();
        switch (status) {
            case SCHEDULED -> {
                if (orderStatus == OrderStatus.PAID) {
                    actions.add(SHIP);
                }
                actions.add(CANCEL);
            }
            case SHIPPED -> {
                if (orderStatus == OrderStatus.SHIPPED) {
                    actions.add(COMPLETE);
                }
            }
            case DELIVERED, CANCELLED -> {
            }
        }
        return List.copyOf(actions);
    }
}
