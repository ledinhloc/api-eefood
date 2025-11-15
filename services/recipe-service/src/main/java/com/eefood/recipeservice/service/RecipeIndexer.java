package com.eefood.recipeservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.eefood.recipeservice.mapper.RecipeMapper;
import com.eefood.recipeservice.model.Recipe;
import com.eefood.recipeservice.model.RecipeDocument;
import com.eefood.recipeservice.repository.RecipeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecipeIndexer {
  private final RecipeRepository recipeRepo;
  private final ElasticsearchClient client;
  private final RecipeMapper recipeMapper;

  /**
   * Khi service khởi động, index toàn bộ dữ liệu recipe hiện có vào Elasticsearch
   */
  @PostConstruct
  public void init() {
    try {
      indexAllRecipes();
    } catch (Exception e) {
      log.error("Failed to index recipes: {}", e.getMessage());
    }
  }

  /**
   * Index toàn bộ recipe (chạy khi start app)
   */
  public void indexAllRecipes() throws IOException {
    List<RecipeDocument> recipes = recipeRepo.findAll().stream()
      .filter(r -> !r.getIsDeleted())
      .map(recipeMapper::toDocument)
      .toList();

    for (RecipeDocument recipe : recipes) {
      client.index(i -> i
        .index("recipes")
        .id(String.valueOf(recipe.getId()))
        .document(recipe)
      );
    }
    log.info("Indexed {} recipes to Elasticsearch", recipes.size());
  }

  /**
   * Them moi hoac cap nhat recipe
   */
  public void saveOrUpdateRecipe(Recipe recipe) {
    try{
      RecipeDocument doc = recipeMapper.toDocument(recipe);
      client.index(i -> i
        .index("recipes")
        .id(String.valueOf(doc.getId()))
        .document(doc)
      );
      log.info("Recipe indexed/updated: {}", doc.getId());
    }catch (IOException e) {
      log.error("Error indexing recipe {}: {}", recipe.getId(), e.getMessage());
    }
  }
  /**
   * Xoa recipe khoi Els
   */
  public void deleteRecipe(Long recipeId) {
    try{
      client.delete(d -> d
        .index("recipes")
        .id(String.valueOf(recipeId))
      );
      log.info("Deleted recipe {} from Elasticsearch", recipeId);
    }catch (IOException e) {
      log.error("Error deleting recipe {}: {}", recipeId, e.getMessage());
    }
  }

  private void clearIndex() throws IOException {
    if (client.indices().exists(e -> e.index("recipes")).value()) {
      client.indices().delete(d -> d.index("recipes"));
      log.warn("Deleted old index 'recipes' before re-indexing");
    }
  }

}
