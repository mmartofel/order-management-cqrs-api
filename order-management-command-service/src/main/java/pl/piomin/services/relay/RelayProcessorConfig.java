package pl.piomin.services.relay;

import org.axonframework.extension.spring.config.EventProcessorDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Puts the relay in its own pooled streaming processor with one segment: one thread publishes the events in the order
 * of the event store, and in a multi-replica deployment only the replica that claims the segment relays.
 */
@Configuration(proxyBeanMethods = false)
public class RelayProcessorConfig {

    public static final String PROCESSOR = "kafka-relay";

    @Bean
    EventProcessorDefinition kafkaRelayProcessor() {
        return EventProcessorDefinition.pooledStreaming(PROCESSOR)
                .assigningHandlers(descriptor -> KafkaEventRelay.class.equals(descriptor.beanType()))
                .customized(config -> config.initialSegmentCount(1).batchSize(50));
    }
}
