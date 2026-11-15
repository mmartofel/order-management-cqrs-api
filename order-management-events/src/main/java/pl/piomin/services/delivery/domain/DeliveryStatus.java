package pl.piomin.services.delivery.domain;

/** States of a delivery; shared by the command side (rules) and the query side (read model, allowed actions). */
public enum DeliveryStatus {
    SCHEDULED, SHIPPED, DELIVERED, CANCELLED
}
