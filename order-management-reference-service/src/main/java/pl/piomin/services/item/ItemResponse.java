package pl.piomin.services.item;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record ItemResponse(@Schema(requiredMode = REQUIRED) Long id,
                           @Schema(requiredMode = REQUIRED) String sku,
                           @Schema(requiredMode = REQUIRED) String name,
                           String description,
                           @Schema(requiredMode = REQUIRED) BigDecimal price,
                           @Schema(requiredMode = REQUIRED) Long version) {

    static ItemResponse of(Item item) {
        return new ItemResponse(item.getId(), item.getSku(), item.getName(), item.getDescription(), item.getPrice(),
                item.getVersion());
    }
}
