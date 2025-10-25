package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.response.CollectionResponse;
import com.eefood.reactionservice.dto.response.PostCollectionResponse;
import com.eefood.reactionservice.dto.response.PostResponse;
import com.eefood.reactionservice.model.Collection;
import com.eefood.reactionservice.model.CollectionPost;
import com.eefood.reactionservice.model.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CollectionMapper {
  @Mapping(target = "posts", expression = "java(toPostResponses(collection.getCollectionPosts()))")
  CollectionResponse toDto(Collection collection);

  default List<PostCollectionResponse> toPostResponses(List<CollectionPost> collectionPosts) {
    if (collectionPosts == null) return List.of();
    return collectionPosts.stream()
        .map(
            cp -> {
              Post post = cp.getPost();
              return PostCollectionResponse.builder()
                  .id(cp.getId())
                  .postId(post.getId())
                  .userId(post.getUserId())
                  .recipeId(post.getRecipeId())
                  .title(post.getTitle())
                  .imageUrl(post.getImageUrl())
                  .createdAt(cp.getCreatedAt())
                  .build();
            })
        .toList();
  }
}
