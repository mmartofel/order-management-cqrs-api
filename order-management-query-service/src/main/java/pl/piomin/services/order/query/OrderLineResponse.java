package pl.piomin.services.order.query;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** An order line with the item's SKU, name and price as they were when the order was placed. */
public record OrderLineResponse(@Schema(requiredMode = REQUIRED) Long itemId,
                                @Schema(requiredMode = REQUIRED) String sku,
                                @Schema(requiredMode = REQUIRED) String name,
                                @Schema(requiredMode = REQUIRED) int quantity,
                                @Schema(requiredMode = REQUIRED) BigDecimal unitPrice) {

    static OrderLineResponse of(OrderLineView line) {
        return new OrderLineResponse(line.getItemId(), line.getSku(), line.getName(), line.getQuantity(),
                line.getUnitPrice());
    }
}
