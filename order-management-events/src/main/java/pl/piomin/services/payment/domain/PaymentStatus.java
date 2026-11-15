package pl.piomin.services.payment.domain;

/** States of a payment; shared by the command side (rules) and the query side (read model, allowed actions). */
public enum PaymentStatus {
    INITIATED, COMPLETED, FAILED, REFUNDED
}
