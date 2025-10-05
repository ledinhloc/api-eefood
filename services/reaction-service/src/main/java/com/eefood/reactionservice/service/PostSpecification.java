package com.eefood.reactionservice.service;

import com.eefood.reactionservice.model.Post;
import org.springframework.data.jpa.domain.Specification;

public class PostSpecification {
  public PostSpecification() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static Specification<Post> hasTitleLike(String title) {
    return (root, query, cb) ->{
      if(title == null || title.isBlank()) return null;
      return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    };
  }

  public static Specification<Post> hasUserId(Long userId) {
    return (root, query, cb) ->{
      if(userId == null) return null;
      return cb.equal(root.get("userId"), userId);
    };
  }

  public static Specification<Post> hasContentLike(String content) {
    return (root, query, cb)->{
      if(content == null || content.isBlank()) return null;
      return cb.like(cb.lower(root.get("content")), "%" + content.toLowerCase() + "%");
    };
  }

  public static Specification<Post> hasRecipeId(Long recipeId) {
    return (root, query, cb)->{
      if(recipeId == null) return null;
      return cb.equal(root.get("recipeId"), recipeId);
    };
  }

}
