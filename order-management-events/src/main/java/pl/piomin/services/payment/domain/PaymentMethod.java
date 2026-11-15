package pl.piomin.services.payment.domain;

/** How a payment is made. Only the method name is stored, never card or account details. */
public enum PaymentMethod {
    CARD, TRANSFER, BLIK, CASH_ON_DELIVERY
}
