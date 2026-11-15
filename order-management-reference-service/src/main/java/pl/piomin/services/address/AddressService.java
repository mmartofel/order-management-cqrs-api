package pl.piomin.services.address;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.piomin.services.api.Reference;
import pl.piomin.services.exception.ApiExceptionHandler;
import pl.piomin.services.person.Person;
import pl.piomin.services.person.PersonRepository;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AddressService {

    private final AddressRepository repository;
    private final PersonRepository personRepository;

    public AddressService(AddressRepository repository, PersonRepository personRepository) {
        this.repository = repository;
        this.personRepository = personRepository;
    }

    /** The owners of the whole page are read in one query. */
    public Page<AddressResponse> findAll(AddressFilter filter, Pageable pageable) {
        Page<Address> page = repository.findAll(filter.toSpecification(), pageable);
        Map<Long, Reference> persons = personRepository.findAllById(page.map(Address::getPersonId).toSet()).stream()
                .collect(Collectors.toMap(Person::getId, AddressService::reference));
        return page.map(address -> AddressResponse.of(address, persons.get(address.getPersonId())));
    }

    public AddressResponse findById(Long id) {
        return response(get(id));
    }

    @Transactional
    public AddressResponse create(AddressCreateRequest request) {
        requirePerson(request.personId());
        Address address = new Address(request.personId(), request.street(), request.city(), request.postalCode(),
                request.country());
        return response(repository.saveAndFlush(address));
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
        return response(repository.saveAndFlush(address));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
        repository.flush();
    }

    /** 400 {@code not-found} on {@code personId}. */
    private void requirePerson(Long personId) {
        if (!personRepository.existsById(personId)) {
            throw ApiExceptionHandler.notFound("personId");
        }
    }

    private AddressResponse response(Address address) {
        return AddressResponse.of(address, personRepository.findById(address.getPersonId())
                .map(AddressService::reference).orElseThrow());
    }

    private static Reference reference(Person person) {
        return new Reference(person.getId(), person.getFirstName() + " " + person.getLastName());
    }

    private Address get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address " + id + " not found"));
    }
}
