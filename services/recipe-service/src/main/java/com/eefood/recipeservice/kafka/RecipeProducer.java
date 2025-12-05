package com.eefood.recipeservice.kafka;

import com.eefood.common.avro.RecipeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecipeProducer {
  private final KafkaTemplate<String, RecipeEvent> kafkaTemplate;

  public void sendRecipe(RecipeEvent event) {
    kafkaTemplate.send("recipe-update-topic", String.valueOf(event.getId()), event);
  }
}