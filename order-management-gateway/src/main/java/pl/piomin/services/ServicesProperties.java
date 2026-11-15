package pl.piomin.services;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/** Where the services behind the gateway run; the routes in {@code application.yml} use the same values. */
@ConfigurationProperties("services")
public record ServicesProperties(URI commandUrl, URI queryUrl, URI referenceUrl, Duration healthTimeout) {
}
