package pl.piomin.services.order.domain.event;

import java.math.BigDecimal;

/** One ordered item, with name and unit price copied from the item when the order was placed. */
public record OrderLine(Long itemId, String sku, String name, int quantity, BigDecimal unitPrice) {

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
