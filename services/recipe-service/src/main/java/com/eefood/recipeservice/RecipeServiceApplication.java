package com.eefood.recipeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
//@EnableKafka
public class RecipeServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(RecipeServiceApplication.class, args);
  }
}
