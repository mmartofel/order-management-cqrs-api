package pl.piomin.services.person;

public record PersonResponse(Long id, String firstName, String lastName, String email, String phone, Long version) {

    static PersonResponse of(Person person) {
        return new PersonResponse(person.getId(), person.getFirstName(), person.getLastName(), person.getEmail(),
                person.getPhone(), person.getVersion());
    }
}
