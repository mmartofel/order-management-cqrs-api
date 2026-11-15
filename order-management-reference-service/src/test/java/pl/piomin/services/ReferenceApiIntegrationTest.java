package pl.piomin.services;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.piomin.services.address.AddressCreateRequest;
import pl.piomin.services.address.AddressResponse;
import pl.piomin.services.api.Reference;
import pl.piomin.services.item.ItemCreateRequest;
import pl.piomin.services.item.ItemResponse;
import pl.piomin.services.item.ItemUpdateRequest;
import pl.piomin.services.person.PersonCreateRequest;
import pl.piomin.services.person.PersonResponse;
import pl.piomin.services.person.PersonUpdateRequest;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import({TestcontainersConfig.class, TestJwtConfig.class})
class ReferenceApiIntegrationTest {

    @Autowired
    RestTestClient client;

    @Autowired
    Flyway flyway;

    @BeforeEach
    void editor() {
        client = client.mutate().defaultHeader(HttpHeaders.AUTHORIZATION, TestJwtConfig.EDITOR_TOKEN).build();
    }

    @Test
    void migrations_allApplied() {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied()).isNotEmpty();
    }

    // --- Person ---

    @Test
    void createPerson_valid_returns201WithLocationAndBody() {
        PersonCreateRequest request = new PersonCreateRequest("Anna", "Nowak", unique("anna"), "+48 600 100 200");
        PersonResponse created = client.post().uri("/api/persons").contentType(MediaType.APPLICATION_JSON)
                .body(request).exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody(PersonResponse.class).returnResult().getResponseBody();

        assertThat(created.id()).isNotNull();
        assertThat(created.email()).isEqualTo(request.email());
        assertThat(created.version()).isZero();
    }

    @Test
    void createPerson_blankNameAndBadEmail_returns400WithFieldErrors() {
        client.post().uri("/api/persons").contentType(MediaType.APPLICATION_JSON)
                .body(new PersonCreateRequest("", "Nowak", "not-an-email", null)).exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.errors[?(@.field == 'firstName')].code").isEqualTo("required")
                .jsonPath("$.errors[?(@.field == 'email')].code").isEqualTo("email");
    }

    @Test
    void createPerson_duplicateEmail_returns409() {
        String email = unique("dup");
        createPerson(email);
        client.post().uri("/api/persons").contentType(MediaType.APPLICATION_JSON)
                .body(new PersonCreateRequest("Other", "Person", email, null)).exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.errors[0].field").isEqualTo("email")
                .jsonPath("$.errors[0].code").isEqualTo("unique");
    }

    @Test
    void getPerson_unknownId_returns404ProblemDetail() {
        client.get().uri("/api/persons/{id}", Long.MAX_VALUE).exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.status").isEqualTo(404);
    }

    @Test
    void updatePerson_currentVersion_returns200WithNewVersion() {
        PersonResponse person = createPerson(unique("upd"));
        PersonResponse updated = client.put().uri("/api/persons/{id}", person.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PersonUpdateRequest("Anna", "Kowalska", person.email(), null, person.version())).exchange()
                .expectStatus().isOk()
                .expectBody(PersonResponse.class).returnResult().getResponseBody();

        assertThat(updated.lastName()).isEqualTo("Kowalska");
        assertThat(updated.version()).isEqualTo(person.version() + 1);
    }

    @Test
    void updatePerson_staleVersion_returns409() {
        PersonResponse person = createPerson(unique("stale"));
        client.put().uri("/api/persons/{id}", person.id()).contentType(MediaType.APPLICATION_JSON)
                .body(new PersonUpdateRequest("Anna", "Kowalska", person.email(), null, person.version() + 5))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void deletePerson_withAddress_returns409AndKeepsPerson() {
        PersonResponse person = createPerson(unique("ref"));
        createAddress(person.id());

        client.delete().uri("/api/persons/{id}", person.id()).exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.errors[0].code").isEqualTo("in-use")
                .jsonPath("$.errors[0].field").doesNotExist();
        client.get().uri("/api/persons/{id}", person.id()).exchange().expectStatus().isOk();
    }

    @Test
    void deletePerson_unreferenced_returns204ThenNotFound() {
        PersonResponse person = createPerson(unique("del"));
        client.delete().uri("/api/persons/{id}", person.id()).exchange().expectStatus().isNoContent();
        client.get().uri("/api/persons/{id}", person.id()).exchange().expectStatus().isNotFound();
    }

    @Test
    void listPersons_returnsPagedModel() {
        createPerson(unique("list"));
        client.get().uri("/api/persons?page=0&size=5").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.page.size").isEqualTo(5)
                .jsonPath("$.page.totalElements").exists();
    }

    @Test
    void listPersons_unknownSortProperty_returns400() {
        client.get().uri("/api/persons?sort=nope").exchange().expectStatus().isBadRequest();
    }

    // --- Address ---

    @Test
    void createAddress_existingPerson_returns201AndIsListedForPerson() {
        PersonResponse person = createPerson(unique("addr"));
        AddressResponse address = createAddress(person.id());

        assertThat(address.person()).isEqualTo(new Reference(person.id(), "Anna Nowak"));
        client.get().uri("/api/addresses?personId={id}", person.id()).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[0].id").isEqualTo(address.id().intValue())
                .jsonPath("$.content[0].person.label").isEqualTo("Anna Nowak");
    }

    @Test
    void createAddress_unknownPerson_returns400NotFoundOnPersonId() {
        client.post().uri("/api/addresses").contentType(MediaType.APPLICATION_JSON)
                .body(new AddressCreateRequest(Long.MAX_VALUE, "Main 1", "Warsaw", "00-001", "PL")).exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0].field").isEqualTo("personId")
                .jsonPath("$.errors[0].code").isEqualTo("not-found");
    }

    @Test
    void listPersons_searchAndIds_filterTheList() {
        PersonResponse first = createPerson(unique("zofia"));
        PersonResponse second = createPerson(unique("zenon"));

        client.get().uri("/api/persons?q={q}", first.email().toUpperCase()).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.page.totalElements").isEqualTo(1)
                .jsonPath("$.content[0].id").isEqualTo(first.id().intValue());
        client.get().uri("/api/persons?ids={a}&ids={b}", first.id(), second.id()).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.page.totalElements").isEqualTo(2);
    }

    @Test
    void viewer_readsButCannotWrite() {
        RestTestClient viewer = client.mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, TestJwtConfig.VIEWER_TOKEN).build();
        viewer.get().uri("/api/persons").exchange().expectStatus().isOk();
        viewer.post().uri("/api/persons").contentType(MediaType.APPLICATION_JSON)
                .body(new PersonCreateRequest("Anna", "Nowak", unique("viewer"), null)).exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void statusEndpointsAndContract_arePublic() {
        RestTestClient anonymous = client.mutate()
                .defaultHeaders(headers -> headers.remove(HttpHeaders.AUTHORIZATION)).build();
        anonymous.get().uri("/actuator/health").exchange().expectStatus().isOk();
        anonymous.get().uri("/actuator/info").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.build.version").exists();
        anonymous.get().uri("/api/persons").exchange().expectStatus().isUnauthorized();
    }

    /**
     * Writes this service's part of the contract to {@code target/openapi.json}, first writing and then asserting, so a
     * failing assertion still leaves the file to inspect. The gateway merges it into {@code contract/openapi.json}.
     */
    @Test
    void exportOpenApiContract() throws Exception {
        String json = client.get().uri("/v3/api-docs").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        Files.writeString(Path.of("target/openapi.json"), json + System.lineSeparator());

        DocumentContext contract = JsonPath.parse(json);
        assertThat(contract.read("$.paths['/api/persons'].post.operationId", String.class)).isEqualTo("createPerson");
        assertThat(contract.read("$.paths['/api/persons'].post.responses", Map.class)).containsKey("201");
        assertThat(contract.read("$.paths['/api/persons/{id}'].delete.responses", Map.class))
                .containsKeys("204", "404", "409");
        assertThat(contract.read("$.paths['/api/addresses'].get.parameters[*].name", List.class))
                .contains("q", "personId", "ids", "page", "size", "sort");
        assertThat(contract.read("$.components.schemas.PersonCreateRequest.properties.firstName.minLength",
                Integer.class)).isEqualTo(1);
        assertThat(contract.read("$.components.schemas.AddressResponse.required", List.class))
                .contains("id", "version", "person");
    }

    @Test
    void createAddress_invalidCountry_returns400() {
        PersonResponse person = createPerson(unique("country"));
        client.post().uri("/api/addresses").contentType(MediaType.APPLICATION_JSON)
                .body(new AddressCreateRequest(person.id(), "Main 1", "Warsaw", "00-001", "Poland")).exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors[0].field").isEqualTo("country");
    }

    // --- Item ---

    @Test
    void createItem_valid_returns201() {
        ItemResponse item = createItem(unique("SKU"), "19.99");
        assertThat(item.price()).isEqualByComparingTo("19.99");
    }

    @Test
    void createItem_duplicateSku_returns409() {
        String sku = unique("SKU");
        createItem(sku, "5.00");
        client.post().uri("/api/items").contentType(MediaType.APPLICATION_JSON)
                .body(new ItemCreateRequest(sku, "Other", null, new BigDecimal("6.00"))).exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.errors[0].field").isEqualTo("sku");
    }

    @Test
    void createItem_nonPositivePrice_returns400() {
        client.post().uri("/api/items").contentType(MediaType.APPLICATION_JSON)
                .body(new ItemCreateRequest(unique("SKU"), "Free", null, BigDecimal.ZERO)).exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors[0].field").isEqualTo("price");
    }

    @Test
    void updateItem_staleVersion_returns409() {
        ItemResponse item = createItem(unique("SKU"), "5.00");
        client.put().uri("/api/items/{id}", item.id()).contentType(MediaType.APPLICATION_JSON)
                .body(new ItemUpdateRequest(item.sku(), "Renamed", null, item.price(), item.version() + 1)).exchange()
                .expectStatus().isEqualTo(409);
    }

    private PersonResponse createPerson(String email) {
        return client.post().uri("/api/persons").contentType(MediaType.APPLICATION_JSON)
                .body(new PersonCreateRequest("Anna", "Nowak", email, null)).exchange()
                .expectStatus().isCreated()
                .expectBody(PersonResponse.class).returnResult().getResponseBody();
    }

    private AddressResponse createAddress(Long personId) {
        return client.post().uri("/api/addresses").contentType(MediaType.APPLICATION_JSON)
                .body(new AddressCreateRequest(personId, "Marszalkowska 1", "Warsaw", "00-001", "PL")).exchange()
                .expectStatus().isCreated()
                .expectBody(AddressResponse.class).returnResult().getResponseBody();
    }

    private ItemResponse createItem(String sku, String price) {
        return client.post().uri("/api/items").contentType(MediaType.APPLICATION_JSON)
                .body(new ItemCreateRequest(sku, "Item " + sku, "desc", new BigDecimal(price))).exchange()
                .expectStatus().isCreated()
                .expectBody(ItemResponse.class).returnResult().getResponseBody();
    }

    private static String unique(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return prefix.equals("SKU") ? prefix + "-" + suffix : prefix + "-" + suffix + "@example.com";
    }
}
