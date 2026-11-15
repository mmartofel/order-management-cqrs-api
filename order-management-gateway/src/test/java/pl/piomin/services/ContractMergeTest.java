package pl.piomin.services;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Merges the contracts the services' tests export ({@code target/openapi.json}) into {@code contract/openapi.json},
 * the one contract the UI is generated from. It runs after the services' tests, because the gateway is the reactor's
 * last module. Every path and method comes from exactly one service; a schema defined by more than one service must
 * be identical in all of them ({@code Problem}, {@code FieldError}, {@code Reference}, ...), or the merge fails.
 * CI then fails when the committed file differs from the merged one.
 */
class ContractMergeTest {

    private static final List<String> SERVICES = List.of("order-management-command-service",
            "order-management-query-service", "order-management-reference-service");

    private static final String DESCRIPTION = """
            Orders, payments and deliveries are command resources: they are created with POST (201) and changed only \
            through actions, POST /{id}/<action> (202), each returning {id, version}. **Reads are eventually \
            consistent:** right after a command a GET may still return the old version, or 404 after a create; read \
            until `version` reaches the command's version. Persons, addresses and items are plain CRUD resources.""";

    private final JsonMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    @Test
    void mergeServiceContracts() throws Exception {
        List<String> conflicts = new ArrayList<>();
        Map<String, JsonNode> paths = new TreeMap<>();
        Map<String, JsonNode> schemas = new TreeMap<>();
        Map<String, JsonNode> securitySchemes = new TreeMap<>();
        Map<String, JsonNode> tags = new TreeMap<>();
        String openApiVersion = null;

        for (String service : SERVICES) {
            Path file = Path.of("..", service, "target", "openapi.json");
            assertThat(file).as("%s exports its contract in its integration test; run the whole reactor", service)
                    .exists();
            JsonNode contract = mapper.readTree(file.toFile());
            openApiVersion = contract.get("openapi").asString();
            contract.get("paths").properties().forEach(path -> {
                ObjectNode merged = (ObjectNode) paths.computeIfAbsent(path.getKey(),
                        key -> JsonNodeFactory.instance.objectNode());
                path.getValue().properties().forEach(operation -> {
                    if (merged.has(operation.getKey())) {
                        conflicts.add(operation.getKey().toUpperCase() + " " + path.getKey() + " in two services");
                    }
                    merged.set(operation.getKey(), operation.getValue());
                });
            });
            mergeNamed(contract.path("components").path("schemas"), schemas, "schema", conflicts);
            mergeNamed(contract.path("components").path("securitySchemes"), securitySchemes, "security scheme",
                    conflicts);
            contract.path("tags").forEach(tag -> tags.putIfAbsent(tag.get("name").asString(), tag));
        }
        assertThat(conflicts).as("contract conflicts between the services").isEmpty();

        ObjectNode merged = JsonNodeFactory.instance.objectNode();
        merged.put("openapi", openApiVersion);
        merged.putObject("info")
                .put("title", "Order Management API")
                .put("version", System.getProperty("project.version"))
                .put("description", DESCRIPTION);
        merged.putArray("servers").addObject().put("url", "/");
        merged.set("paths", object(paths));
        ObjectNode components = merged.putObject("components");
        components.set("schemas", object(schemas));
        components.set("securitySchemes", object(securitySchemes));
        if (!tags.isEmpty()) {
            ArrayNode tagList = merged.putArray("tags");
            tags.values().forEach(tagList::add);
        }

        Path target = Path.of("..", "contract", "openapi.json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, mapper.writeValueAsString(sorted(merged)) + System.lineSeparator());

        JsonNode written = mapper.readTree(target.toFile());
        assertThat(written.at("/paths/~1api~1orders/get/operationId").asString()).isEqualTo("listOrders");
        assertThat(written.at("/paths/~1api~1orders/post/operationId").asString()).isEqualTo("createOrder");
        assertThat(written.at("/paths/~1api~1persons~1{id}/put/operationId").asString()).isEqualTo("updatePerson");
        assertThat(written.at("/info/version").asString()).isEqualTo(System.getProperty("project.version"));
    }

    private static void mergeNamed(JsonNode source, Map<String, JsonNode> target, String kind, List<String> conflicts) {
        source.properties().forEach(entry -> {
            JsonNode existing = target.putIfAbsent(entry.getKey(), entry.getValue());
            if (existing != null && !existing.equals(entry.getValue())) {
                conflicts.add(kind + " " + entry.getKey() + " differs between services");
            }
        });
    }

    private static ObjectNode object(Map<String, JsonNode> entries) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        entries.forEach(node::set);
        return node;
    }

    /** Keys in alphabetical order at every level, like the services' own contracts: a stable file, readable diffs. */
    private static JsonNode sorted(JsonNode node) {
        if (node.isObject()) {
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            new TreeMap<>(node.propertyStream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    .forEach((key, value) -> result.set(key, sorted(value)));
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = JsonNodeFactory.instance.arrayNode();
            node.forEach(element -> result.add(sorted(element)));
            return result;
        }
        return node;
    }
}
