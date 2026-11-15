package pl.piomin.services.order.domain;

/** States of an order; shared by the command side (rules) and the query side (read model, allowed actions). */
public enum OrderStatus {
    PLACED, PAID, SHIPPED, DELIVERED, CANCELLED
}
