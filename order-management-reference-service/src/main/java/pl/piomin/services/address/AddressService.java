package pl.piomin.services.address;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.piomin.services.person.PersonRepository;

@Service
@Transactional(readOnly = true)
public class AddressService {

    private final AddressRepository repository;
    private final PersonRepository personRepository;

    public AddressService(AddressRepository repository, PersonRepository personRepository) {
        this.repository = repository;
        this.personRepository = personRepository;
    }

    public Page<AddressResponse> findAll(Long personId, Pageable pageable) {
        Page<Address> page = personId == null
                ? repository.findAll(pageable)
                : repository.findByPersonId(personId, pageable);
        return page.map(AddressResponse::of);
    }

    public AddressResponse findById(Long id) {
        return AddressResponse.of(get(id));
    }

    @Transactional
    public AddressResponse create(AddressCreateRequest request) {
        requirePerson(request.personId());
        Address address = new Address(request.personId(), request.street(), request.city(), request.postalCode(),
                request.country());
        return AddressResponse.of(repository.saveAndFlush(address));
    }

    @Transactional
    public AddressResponse update(Long id, AddressUpdateRequest request) {
        Address address = get(id);
        if (!address.getVersion().equals(request.version())) {
            throw new ObjectOptimisticLockingFailureException(Address.class, id);
        }
        requirePerson(request.personId());
        address.setPersonId(request.personId());
        address.setStreet(request.street());
        address.setCity(request.city());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());
        return AddressResponse.of(repository.saveAndFlush(address));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
        repository.flush();
    }

    private void requirePerson(Long personId) {
        if (!personRepository.existsById(personId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Person " + personId + " does not exist");
        }
    }

    private Address get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address " + id + " not found"));
    }
}
