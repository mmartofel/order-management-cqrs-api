package pl.piomin.services.order.domain;

import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.modelling.StateManager;
import pl.piomin.services.exception.InvalidReferenceException;

import java.util.UUID;

/** Loads the order a payment or delivery belongs to, inside the command's processing context. */
public final class Orders {

    private Orders() {
    }

    public static Order load(UUID orderId, ProcessingContext context) {
        Order order = context.component(StateManager.class).loadEntity(Order.class, orderId, context).join();
        if (order == null || order.id() == null) {
            throw new InvalidReferenceException("orderId", "Order " + orderId + " does not exist");
        }
        return order;
    }
}
