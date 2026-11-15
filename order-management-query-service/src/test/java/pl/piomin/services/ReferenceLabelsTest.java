package pl.piomin.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import pl.piomin.services.api.Reference;
import pl.piomin.services.reference.ReferenceClient;
import pl.piomin.services.reference.ReferenceLabels;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The real {@link ReferenceClient} reading responses shaped like the reference service's: the integration tests mock
 * the client, so the wire format is checked here.
 */
class ReferenceLabelsTest {

    private MockRestServiceServer server;
    private ReferenceLabels labels;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://reference");
        server = MockRestServiceServer.bindTo(builder).build();
        ReferenceClient client = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build())).build()
                .createClient(ReferenceClient.class);
        labels = new ReferenceLabels(client);
    }

    @Test
    void persons_readsThePagedModelOfTheReferenceService() {
        server.expect(requestTo(startsWith("http://reference/api/persons")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("ids", "7"))
                .andRespond(withSuccess("""
                        {"content":[{"id":7,"firstName":"Anna","lastName":"Nowak","email":"anna@example.com",
                                     "phone":null,"version":0}],
                         "page":{"size":100,"number":0,"totalElements":1,"totalPages":1}}""",
                        MediaType.APPLICATION_JSON));

        assertThat(labels.persons(List.of(7L)).apply(7L)).isEqualTo(new Reference(7L, "Anna Nowak"));
        server.verify();
    }

    @Test
    void addresses_readsThePersonSummaryShapeToo() {
        server.expect(requestTo(startsWith("http://reference/api/addresses")))
                .andRespond(withSuccess("""
                        {"content":[{"id":2,"person":{"id":7,"label":"Anna Nowak"},"street":"Marszalkowska 1",
                                     "city":"Warsaw","postalCode":"00-001","country":"PL","version":0}],
                         "page":{"size":100,"number":0,"totalElements":1,"totalPages":1}}""",
                        MediaType.APPLICATION_JSON));

        assertThat(labels.addresses(List.of(2L)).apply(2L))
                .isEqualTo(new Reference(2L, "Marszalkowska 1, 00-001 Warsaw"));
    }

    @Test
    void referenceServiceDown_givesGenericLabels() {
        server.expect(requestTo(startsWith("http://reference/api/persons")))
                .andRespond(withServerError());

        assertThat(labels.persons(List.of(7L)).apply(7L)).isEqualTo(new Reference(7L, "Person 7"));
    }
}
