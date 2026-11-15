package pl.piomin.services;

import org.junit.jupiter.api.Test;
import pl.piomin.services.reference.ReferenceClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The integration tests replace the reference service with a mock, so they cannot see it change. This test reads the
 * contract the reference service's own tests exported (it is built earlier in the reactor) and checks that every field
 * this service's {@link ReferenceClient} reads exists there, nested records included.
 */
class ReferenceClientContractTest {

    private static final Path CONTRACT = Path.of("..", "order-management-reference-service", "target", "openapi.json");

    @Test
    void referenceClientReadsOnlyFieldsTheReferenceServiceReturns() throws Exception {
        assertThat(CONTRACT).as("the reference service exports its contract in its tests; run the whole reactor")
                .exists();
        JsonNode schemas = JsonMapper.builder().build().readTree(CONTRACT.toFile()).at("/components/schemas");
        MAPPINGS.forEach((record, schema) -> assertFields(schemas, record, schemas.get(schema), schema));
    }

    private static void assertFields(JsonNode schemas, Class<?> record, JsonNode schema, String name) {
        assertThat(schema).as("schema %s in the reference contract", name).isNotNull();
        for (RecordComponent component : record.getRecordComponents()) {
            JsonNode property = schema.path("properties").get(component.getName());
            assertThat(property).as("%s.%s is in the reference contract's %s", record.getSimpleName(),
                    component.getName(), name).isNotNull();
            if (component.getType().isRecord() && property.has("$ref")) {
                String ref = property.get("$ref").asString().substring("#/components/schemas/".length());
                assertFields(schemas, component.getType(), schemas.get(ref), ref);
            }
        }
    }

    /** Each record the client reads, and the reference service's response schema it is read from. */
    private static final Map<Class<?>, String> MAPPINGS = Map.of(
            ReferenceClient.PersonRef.class, "PersonResponse",
            ReferenceClient.AddressRef.class, "AddressResponse");
}
