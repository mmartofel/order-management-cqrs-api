package pl.piomin.services;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;

/**
 * One health component per service behind the gateway ({@code commandService}, {@code queryService},
 * {@code referenceService}), each the service's own readiness. The UI footer shows the API as degraded and names the
 * part that is down.
 */
@Configuration(proxyBeanMethods = false)
public class ServiceHealthConfig {

    @Bean
    HealthIndicator commandServiceHealthIndicator(RestClient.Builder builder, ServicesProperties services) {
        return readiness(builder, services, services.commandUrl());
    }

    @Bean
    HealthIndicator queryServiceHealthIndicator(RestClient.Builder builder, ServicesProperties services) {
        return readiness(builder, services, services.queryUrl());
    }

    @Bean
    HealthIndicator referenceServiceHealthIndicator(RestClient.Builder builder, ServicesProperties services) {
        return readiness(builder, services, services.referenceUrl());
    }

    private static HealthIndicator readiness(RestClient.Builder builder, ServicesProperties services, URI baseUrl) {
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(services.healthTimeout())
                .withReadTimeout(services.healthTimeout());
        RestClient client = builder.clone().baseUrl(baseUrl.toString())
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
        return () -> {
            try {
                client.get().uri("/actuator/health/readiness").retrieve().toBodilessEntity();
                return Health.up().build();
            } catch (RestClientException e) {
                // no details: they would name hosts of the infrastructure
                return Health.down().build();
            }
        };
    }
}
