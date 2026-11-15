package pl.piomin.services.person;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PersonService {

    private final PersonRepository repository;

    public PersonService(PersonRepository repository) {
        this.repository = repository;
    }

    public Page<PersonResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(PersonResponse::of);
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

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
        repository.flush();
    }

    private Person get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person " + id + " not found"));
    }
}
