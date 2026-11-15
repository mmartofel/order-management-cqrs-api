package pl.piomin.services.reference;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import pl.piomin.services.exception.InvalidReferenceException;
import pl.piomin.services.reference.ReferenceClient.AddressRef;
import pl.piomin.services.reference.ReferenceClient.ItemRef;

import java.util.function.Supplier;

/** Checks references against the reference service; a missing record becomes an {@link InvalidReferenceException}. */
@Component
public class ReferenceData {

    private final ReferenceClient client;

    public ReferenceData(ReferenceClient client) {
        this.client = client;
    }

    public void requirePerson(Long personId) {
        fetch(() -> client.person(personId), "Person " + personId + " does not exist");
    }

    /** The address must exist and belong to the given person. */
    public AddressRef requireAddressOf(Long addressId, Long personId) {
        AddressRef address = fetch(() -> client.address(addressId), "Address " + addressId + " does not exist");
        if (!address.personId().equals(personId)) {
            throw new InvalidReferenceException("Address " + addressId + " does not belong to person " + personId);
        }
        return address;
    }

    public ItemRef requireItem(Long itemId) {
        return fetch(() -> client.item(itemId), "Item " + itemId + " does not exist");
    }

    private static <T> T fetch(Supplier<T> call, String notFoundMessage) {
        try {
            return call.get();
        } catch (HttpClientErrorException.NotFound e) {
            throw new InvalidReferenceException(notFoundMessage);
        }
    }
}
