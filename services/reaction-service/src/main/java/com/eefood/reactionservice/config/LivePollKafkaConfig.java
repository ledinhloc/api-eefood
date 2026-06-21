package com.eefood.reactionservice.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class LivePollKafkaConfig {

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> livePollKafkaListenerContainerFactory(
    ConsumerFactory<String, Object> consumerFactory,
    KafkaTemplate<String, Object> kafkaTemplate
  ) {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
    factory.setConsumerFactory(consumerFactory);
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

    var recoverer = new DeadLetterPublishingRecoverer(
      kafkaTemplate,
      (record, exception) -> new TopicPartition(
        record.topic() + ".DLT",
        record.partition()
      )
    );
    factory.setCommonErrorHandler(
      new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L))
    );
    return factory;
  }
}
