package pl.piomin.services.relay;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import pl.piomin.services.delivery.domain.event.DeliveryEvent;
import pl.piomin.services.order.domain.event.OrderEvent;
import pl.piomin.services.payment.domain.event.PaymentEvent;

/**
 * One topic per aggregate type, owned by the command service. No retention limit, so the query service can rebuild its
 * projections by replaying a topic. Never raise the partition count of these topics: the key-to-partition mapping
 * would change and the order of an aggregate's events would be lost.
 */
@Configuration(proxyBeanMethods = false)
public class KafkaTopicConfig {

    @Bean
    NewTopic orderEvents(RelayProperties properties) {
        return topic(OrderEvent.TOPIC, properties);
    }

    @Bean
    NewTopic paymentEvents(RelayProperties properties) {
        return topic(PaymentEvent.TOPIC, properties);
    }

    @Bean
    NewTopic deliveryEvents(RelayProperties properties) {
        return topic(DeliveryEvent.TOPIC, properties);
    }

    private static NewTopic topic(String name, RelayProperties properties) {
        return TopicBuilder.name(name).partitions(properties.partitions()).replicas(properties.replicas())
                .config(TopicConfig.RETENTION_MS_CONFIG, "-1")
                .build();
    }
}
