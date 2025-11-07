package com.eefood.reactionservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;

@Configuration
public class KafkaProducerConfig {
//  @Value("${spring.kafka.bootstrap-servers}")
//  private String bootstrapServers;
//
//  @Bean
//  public ProducerFactory<String, Object> producerConfig() {
//    HashMap<String, Object> props = new HashMap<>();
//    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
//    props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
//    return new DefaultKafkaProducerFactory<>(props);
//  }
//
//  @Bean
//  public KafkaTemplate<String, Object> kafkaTemplate(
//      ProducerFactory<String, Object> producerFactory) {
//    return new KafkaTemplate<>(producerFactory);
//  }
}
