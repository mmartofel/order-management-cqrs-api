package pl.piomin.services.item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository repository;

    public ItemService(ItemRepository repository) {
        this.repository = repository;
    }

    public Page<ItemResponse> findAll(ItemFilter filter, Pageable pageable) {
        return repository.findAll(filter.toSpecification(), pageable).map(ItemResponse::of);
    }

    public ItemResponse findById(Long id) {
        return ItemResponse.of(get(id));
    }

    @Transactional
    public ItemResponse create(ItemCreateRequest request) {
        Item item = new Item(request.sku(), request.name(), request.description(), request.price());
        return ItemResponse.of(repository.saveAndFlush(item));
    }

    @Transactional
    public ItemResponse update(Long id, ItemUpdateRequest request) {
        Item item = get(id);
        if (!item.getVersion().equals(request.version())) {
            throw new ObjectOptimisticLockingFailureException(Item.class, id);
        }
        item.setSku(request.sku());
        item.setName(request.name());
        item.setDescription(request.description());
        item.setPrice(request.price());
        return ItemResponse.of(repository.saveAndFlush(item));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
        repository.flush();
    }

    private Item get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item " + id + " not found"));
    }
}
