package com.chuanqing.config;

import io.micrometer.core.instrument.MeterRegistry;
import com.chuanqing.utils.OutboxTopicUtils;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaReliabilityConfig {

    @Bean
    public NewTopic canalOutboxTopic() {
        return TopicBuilder.name(OutboxTopicUtils.CANAL_OUTBOX).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic canalOutboxDeadLetterTopic() {
        return TopicBuilder.name(OutboxTopicUtils.CANAL_OUTBOX + ".DLT").partitions(1).replicas(1).build();
    }

    @Bean
    public CommonErrorHandler kafkaCommonErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            MeterRegistry meterRegistry
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> {
                    meterRegistry.counter(
                            "zhiguang.kafka.deadletter.published",
                            "topic",
                            record.topic()
                    ).increment();
                    return new TopicPartition(record.topic() + ".DLT", record.partition());
                }
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(1000L, 3L)
        );
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        errorHandler.setCommitRecovered(true);
        errorHandler.setAckAfterHandle(true);
        return errorHandler;
    }
}
