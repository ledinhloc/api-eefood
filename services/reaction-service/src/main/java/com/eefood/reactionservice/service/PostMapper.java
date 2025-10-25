package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.response.CommentResponse;
import com.eefood.reactionservice.dto.response.PostResponse;
import com.eefood.reactionservice.model.Comment;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.model.PostReaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface PostMapper {
  @Mapping(target = "reactionCounts", source = "reactions", qualifiedByName = "mapReactionCounts")
  @Mapping(target = "totalShares", expression = "java((long) post.getShares().size())")
//  @Mapping(target = "comments", expression = "java(mapComments(post.getComments()))")
  PostResponse toResponse(Post post);
//  Post toEntity(PostResponse postResponse);

  // ========== SUPPORT MAPPING METHODS ==========
  @Named("mapReactionCounts")
  default Map<String, Long> mapReactionCounts(List<PostReaction> reactions) {
    if (reactions == null) return Map.of();
    return reactions.stream()
      .collect(Collectors.groupingBy(
        r -> r.getReactionType().name(),
        Collectors.counting()
      ));
  }

  default List<CommentResponse> mapComments(List<Comment> comments) {
    if (comments == null) return List.of();
    return comments.stream()
      .filter(c -> c.getParent() == null)
      .map(this::mapCommentResponse)
      .collect(Collectors.toList());
  }

  default CommentResponse mapCommentResponse(Comment comment) {
    Map<String, Long> reactionCounts = comment.getReactions().stream()
      .collect(Collectors.groupingBy(
        r -> r.getReactionType().name(),
        Collectors.counting()
      ));
    return CommentResponse.builder()
      .id(comment.getId())
      .userId(comment.getUserId())
      .content(comment.getContent())
      .createdAt(comment.getCreatedAt())
      .reactionCounts(reactionCounts)
      .replies(comment.getReplies().stream()
        .map(this::mapCommentResponse)
        .collect(Collectors.toList()))
      .build();
  }
}
