package com.eefood.notificationservice.config;

import java.util.HashMap;
import java.util.Map;

import com.eefood.notificationservice.dto.request.NotificationRequest;
import com.eefood.notificationservice.dto.request.UserNotificationResquest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

@Configuration
public class KafkaConsumerConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  private Map<String, Object> baseConsumerProps() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-group");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
    return props;
  }

  @Bean
  public ConsumerFactory<String, NotificationRequest> notificationRequestConsumerFactory() {
    Map<String, Object> props = baseConsumerProps();
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, NotificationRequest.class.getName());

    return new DefaultKafkaConsumerFactory<>(
            props,
            new StringDeserializer(),
            new JsonDeserializer<>(NotificationRequest.class)
    );
  }

  @Bean
  public ConsumerFactory<String, UserNotificationResquest> userNotificationConsumerFactory() {
    Map<String, Object> props = baseConsumerProps();
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, UserNotificationResquest.class.getName());

    return new DefaultKafkaConsumerFactory<>(
            props,
            new StringDeserializer(),
            new JsonDeserializer<>(UserNotificationResquest.class)
    );
  }

  @Bean(name = "notificationKafkaListenerContainerFactory")
  public ConcurrentKafkaListenerContainerFactory<String, NotificationRequest> notificationKafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, NotificationRequest> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(notificationRequestConsumerFactory());
    return factory;
  }

  @Bean(name = "userNotificationKafkaListenerContainerFactory")
  public ConcurrentKafkaListenerContainerFactory<String, UserNotificationResquest> userNotificationKafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, UserNotificationResquest> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(userNotificationConsumerFactory());
    return factory;
  }
}