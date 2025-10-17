package com.eefood.reactionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableFeignClients
@EnableKafka
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class ReactionServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ReactionServiceApplication.class, args);
  }
}
