package pl.piomin.services.reference;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.time.Duration;

/** Where the reference service (Person, Address, Item) runs. */
@ConfigurationProperties("reference")
public record ReferenceProperties(@DefaultValue("http://localhost:8082") URI baseUrl,
                                  @DefaultValue("2s") Duration connectTimeout,
                                  @DefaultValue("5s") Duration readTimeout) {
}
