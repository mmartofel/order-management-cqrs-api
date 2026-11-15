package pl.piomin.services.config;

import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Maps the {@code event-type} header (the record's simple name) to the event class of one aggregate. Unknown types
 * are reported as empty, so an older query service keeps working when the command service adds an event type.
 */
public final class EventTypes<E> {

    private final Class<E> sealedType;
    private final Map<String, Class<?>> types;
    private final JsonMapper jsonMapper;

    public EventTypes(Class<E> sealedType, JsonMapper jsonMapper) {
        this.sealedType = sealedType;
        this.types = Arrays.stream(sealedType.getPermittedSubclasses())
                .collect(Collectors.toUnmodifiableMap(Class::getSimpleName, Function.identity()));
        this.jsonMapper = jsonMapper;
    }

    public Optional<E> read(EventRecord record) {
        Class<?> type = types.get(record.type());
        return type == null
                ? Optional.empty()
                : Optional.of(sealedType.cast(jsonMapper.readValue(record.payload(), type)));
    }
}
