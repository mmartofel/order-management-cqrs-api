package pl.piomin.services;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.piomin.services.address.AddressCreateRequest;
import pl.piomin.services.address.AddressResponse;
import pl.piomin.services.item.ItemCreateRequest;
import pl.piomin.services.item.ItemResponse;
import pl.piomin.services.item.ItemUpdateRequest;
import pl.piomin.services.person.PersonCreateRequest;
import pl.piomin.services.person.PersonResponse;
import pl.piomin.services.person.PersonUpdateRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfig.class)
class ReferenceApiIntegrationTest {

    @Autowired
    RestTestClient client;

    @Autowired
    Flyway flyway;

    @Test
    void migrations_allApplied() {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied()).isNotEmpty();
    }

    // --- Person ---

    @Test
    void createPerson_valid_returns201WithLocationAndBody() {
        PersonCreateRequest request = new PersonCreateRequest("Anna", "Nowak", unique("anna"), "+48 600 100 200");
        PersonResponse created = client.post().uri("/api/v1/persons").contentType(MediaType.APPLICATION_JSON)
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
        client.post().uri("/api/v1/persons").contentType(MediaType.APPLICATION_JSON)
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
        client.post().uri("/api/v1/persons").contentType(MediaType.APPLICATION_JSON)
                .body(new PersonCreateRequest("Other", "Person", email, null)).exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.errors[0].field").isEqualTo("email")
                .jsonPath("$.errors[0].code").isEqualTo("unique");
    }

    @Test
    void getPerson_unknownId_returns404ProblemDetail() {
        client.get().uri("/api/v1/persons/{id}", Long.MAX_VALUE).exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.status").isEqualTo(404);
    }

    @Test
    void updatePerson_currentVersion_returns200WithNewVersion() {
        PersonResponse person = createPerson(unique("upd"));
        PersonResponse updated = client.put().uri("/api/v1/persons/{id}", person.id())
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
        client.put().uri("/api/v1/persons/{id}", person.id()).contentType(MediaType.APPLICATION_JSON)
                .body(new PersonUpdateRequest("Anna", "Kowalska", person.email(), null, person.version() + 5))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void deletePerson_withAddress_returns409AndKeepsPerson() {
        PersonResponse person = createPerson(unique("ref"));
        createAddress(person.id());

        client.delete().uri("/api/v1/persons/{id}", person.id()).exchange().expectStatus().isEqualTo(409);
        client.get().uri("/api/v1/persons/{id}", person.id()).exchange().expectStatus().isOk();
    }

    @Test
    void deletePerson_unreferenced_returns204ThenNotFound() {
        PersonResponse person = createPerson(unique("del"));
        client.delete().uri("/api/v1/persons/{id}", person.id()).exchange().expectStatus().isNoContent();
        client.get().uri("/api/v1/persons/{id}", person.id()).exchange().expectStatus().isNotFound();
    }

    @Test
    void listPersons_returnsPagedModel() {
        createPerson(unique("list"));
        client.get().uri("/api/v1/persons?page=0&size=5").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.page.size").isEqualTo(5)
                .jsonPath("$.page.totalElements").exists();
    }

    @Test
    void listPersons_unknownSortProperty_returns400() {
        client.get().uri("/api/v1/persons?sort=nope").exchange().expectStatus().isBadRequest();
    }

    // --- Address ---

    @Test
    void createAddress_existingPerson_returns201AndIsListedForPerson() {
        PersonResponse person = createPerson(unique("addr"));
        AddressResponse address = createAddress(person.id());

        assertThat(address.personId()).isEqualTo(person.id());
        client.get().uri("/api/v1/addresses?personId={id}", person.id()).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.content[0].id").isEqualTo(address.id().intValue());
    }

    @Test
    void createAddress_unknownPerson_returns422() {
        client.post().uri("/api/v1/addresses").contentType(MediaType.APPLICATION_JSON)
                .body(new AddressCreateRequest(Long.MAX_VALUE, "Main 1", "Warsaw", "00-001", "PL")).exchange()
                .expectStatus().isEqualTo(422);
    }

    @Test
    void createAddress_invalidCountry_returns400() {
        PersonResponse person = createPerson(unique("country"));
        client.post().uri("/api/v1/addresses").contentType(MediaType.APPLICATION_JSON)
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
        client.post().uri("/api/v1/items").contentType(MediaType.APPLICATION_JSON)
                .body(new ItemCreateRequest(sku, "Other", null, new BigDecimal("6.00"))).exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.errors[0].field").isEqualTo("sku");
    }

    @Test
    void createItem_nonPositivePrice_returns400() {
        client.post().uri("/api/v1/items").contentType(MediaType.APPLICATION_JSON)
                .body(new ItemCreateRequest(unique("SKU"), "Free", null, BigDecimal.ZERO)).exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors[0].field").isEqualTo("price");
    }

    @Test
    void updateItem_staleVersion_returns409() {
        ItemResponse item = createItem(unique("SKU"), "5.00");
        client.put().uri("/api/v1/items/{id}", item.id()).contentType(MediaType.APPLICATION_JSON)
                .body(new ItemUpdateRequest(item.sku(), "Renamed", null, item.price(), item.version() + 1)).exchange()
                .expectStatus().isEqualTo(409);
    }

    private PersonResponse createPerson(String email) {
        return client.post().uri("/api/v1/persons").contentType(MediaType.APPLICATION_JSON)
                .body(new PersonCreateRequest("Anna", "Nowak", email, null)).exchange()
                .expectStatus().isCreated()
                .expectBody(PersonResponse.class).returnResult().getResponseBody();
    }

    private AddressResponse createAddress(Long personId) {
        return client.post().uri("/api/v1/addresses").contentType(MediaType.APPLICATION_JSON)
                .body(new AddressCreateRequest(personId, "Marszalkowska 1", "Warsaw", "00-001", "PL")).exchange()
                .expectStatus().isCreated()
                .expectBody(AddressResponse.class).returnResult().getResponseBody();
    }

    private ItemResponse createItem(String sku, String price) {
        return client.post().uri("/api/v1/items").contentType(MediaType.APPLICATION_JSON)
                .body(new ItemCreateRequest(sku, "Item " + sku, "desc", new BigDecimal(price))).exchange()
                .expectStatus().isCreated()
                .expectBody(ItemResponse.class).returnResult().getResponseBody();
    }

    private static String unique(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return prefix.equals("SKU") ? prefix + "-" + suffix : prefix + "-" + suffix + "@example.com";
    }
}
