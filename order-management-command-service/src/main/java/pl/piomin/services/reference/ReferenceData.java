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
        fetch(() -> client.person(personId), "personId", "Person " + personId + " does not exist");
    }

    /** The address must exist and belong to the given person. */
    public AddressRef requireAddressOf(Long addressId, Long personId) {
        AddressRef address = fetch(() -> client.address(addressId), "addressId",
                "Address " + addressId + " does not exist");
        // an address of someone else is reported like a missing one: it does not exist for this person
        if (!address.person().id().equals(personId)) {
            throw new InvalidReferenceException("addressId",
                    "Address " + addressId + " does not belong to person " + personId);
        }
        return address;
    }

    /** @param field the request field that holds the id, for example {@code lines[2].itemId} */
    public ItemRef requireItem(Long itemId, String field) {
        return fetch(() -> client.item(itemId), field, "Item " + itemId + " does not exist");
    }

    private static <T> T fetch(Supplier<T> call, String field, String notFoundMessage) {
        try {
            return call.get();
        } catch (HttpClientErrorException.NotFound e) {
            throw new InvalidReferenceException(field, notFoundMessage);
        }
    }
}
