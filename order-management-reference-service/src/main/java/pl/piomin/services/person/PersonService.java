package pl.piomin.services.person;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.piomin.services.address.AddressRepository;
import pl.piomin.services.exception.ApiExceptionHandler;

@Service
@Transactional(readOnly = true)
public class PersonService {

    private final PersonRepository repository;
    private final AddressRepository addressRepository;

    public PersonService(PersonRepository repository, AddressRepository addressRepository) {
        this.repository = repository;
        this.addressRepository = addressRepository;
    }

    public Page<PersonResponse> findAll(PersonFilter filter, Pageable pageable) {
        return repository.findAll(filter.toSpecification(), pageable).map(PersonResponse::of);
    }

    public PersonResponse findById(Long id) {
        return PersonResponse.of(get(id));
    }

    @Transactional
    public PersonResponse create(PersonCreateRequest request) {
        Person person = new Person(request.firstName(), request.lastName(), request.email(), request.phone());
        return PersonResponse.of(repository.saveAndFlush(person));
    }

    @Transactional
    public PersonResponse update(Long id, PersonUpdateRequest request) {
        Person person = get(id);
        if (!person.getVersion().equals(request.version())) {
            throw new ObjectOptimisticLockingFailureException(Person.class, id);
        }
        person.setFirstName(request.firstName());
        person.setLastName(request.lastName());
        person.setEmail(request.email());
        person.setPhone(request.phone());
        return PersonResponse.of(repository.saveAndFlush(person));
    }

    /** A person with addresses is not deleted: 409 {@code in-use}. Delete the addresses first. */
    @Transactional
    public void delete(Long id) {
        Person person = get(id);
        if (addressRepository.countByPersonId(id) > 0) {
            throw ApiExceptionHandler.inUse("Person " + id + " still has addresses", "has addresses");
        }
        repository.delete(person);
        repository.flush();
    }

    private Person get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person " + id + " not found"));
    }
}
