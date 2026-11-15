package pl.piomin.services.item;

import java.math.BigDecimal;

public record ItemResponse(Long id, String sku, String name, String description, BigDecimal price, Long version) {

    static ItemResponse of(Item item) {
        return new ItemResponse(item.getId(), item.getSku(), item.getName(), item.getDescription(), item.getPrice(),
                item.getVersion());
    }
}
