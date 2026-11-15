package pl.piomin.services.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import pl.piomin.services.delivery.domain.event.DeliveryEvent;
import pl.piomin.services.order.domain.event.OrderEvent;
import pl.piomin.services.payment.domain.event.PaymentEvent;

/**
 * Three retries one second apart, then the record goes to {@code <topic>-dlt} and the listener moves on. A record
 * there leaves a read model behind: alert on the dead-letter topics, fix the cause, then rebuild the projection.
 */
@Configuration(proxyBeanMethods = false)
public class KafkaConsumerConfig {

    /** At least as many partitions as the event topics: a dead letter goes to the same partition number. */
    static final int DLT_PARTITIONS = 3;

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafka) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafka), new FixedBackOff(1000L, 3));
    }

    @Bean
    NewTopic orderEventsDeadLetter() {
        return TopicBuilder.name(OrderEvent.TOPIC + "-dlt").partitions(DLT_PARTITIONS).replicas(1).build();
    }

    @Bean
    NewTopic paymentEventsDeadLetter() {
        return TopicBuilder.name(PaymentEvent.TOPIC + "-dlt").partitions(DLT_PARTITIONS).replicas(1).build();
    }

    @Bean
    NewTopic deliveryEventsDeadLetter() {
        return TopicBuilder.name(DeliveryEvent.TOPIC + "-dlt").partitions(DLT_PARTITIONS).replicas(1).build();
    }
}
