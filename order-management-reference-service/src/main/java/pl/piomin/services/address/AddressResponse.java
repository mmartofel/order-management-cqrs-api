package pl.piomin.services.address;

public record AddressResponse(Long id, Long personId, String street, String city, String postalCode, String country,
                              Long version) {

    static AddressResponse of(Address address) {
        return new AddressResponse(address.getId(), address.getPersonId(), address.getStreet(), address.getCity(),
                address.getPostalCode(), address.getCountry(), address.getVersion());
    }
}
