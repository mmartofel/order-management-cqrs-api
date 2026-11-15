package pl.piomin.services.relay;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** Topic layout and broker acknowledgement timeout of the Kafka relay. */
@ConfigurationProperties("relay")
public record RelayProperties(@DefaultValue("3") int partitions,
                              @DefaultValue("1") short replicas,
                              @DefaultValue("10s") Duration sendTimeout) {
}
