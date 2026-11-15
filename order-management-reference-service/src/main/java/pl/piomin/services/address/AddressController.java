package pl.piomin.services.address;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** CRUD of addresses; the method names are the operationIds of the contract. */
@RestController
@RequestMapping("/api/addresses")
@Tag(name = "Addresses")
@SecurityRequirement(name = "bearer")
public class AddressController {

    private final AddressService service;

    public AddressController(AddressService service) {
        this.service = service;
    }

    @GetMapping
    public PagedModel<AddressResponse> listAddresses(@ParameterObject @Valid AddressFilter filter,
                                                     @ParameterObject Pageable pageable) {
        return new PagedModel<>(service.findAll(filter, pageable));
    }

    @GetMapping("/{id}")
    public AddressResponse getAddress(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AddressResponse> createAddress(@Valid @RequestBody AddressCreateRequest request) {
        AddressResponse address = service.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(address.id()).toUri()).body(address);
    }

    @PutMapping("/{id}")
    public AddressResponse updateAddress(@PathVariable Long id, @Valid @RequestBody AddressUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@PathVariable Long id) {
        service.delete(id);
    }
}
