package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.response.CommentResponse;
import com.eefood.reactionservice.dto.response.PostPublishResponse;
import com.eefood.reactionservice.dto.response.PostResponse;
import com.eefood.reactionservice.model.Comment;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.model.PostDocument;
import com.eefood.reactionservice.model.PostReaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface PostMapper {
  @Mapping(target = "countReaction", expression = "java(post.getReactions() != null ? post.getReactions().size() : 0L)")
  @Mapping(target = "countComment", expression = "java(post.getComments() != null ? post.getComments().size() : 0L)")
  @Mapping(target = "difficulty", expression = "java(post.getDifficulty() != null ? post.getDifficulty().name() : null)")
  @Mapping(target = "location", source = "region")
  @Mapping(target = "prepTime", expression = "java(post.getPrepTime() != null ? post.getPrepTime().toString() : null)")
  @Mapping(target = "cookTime", expression = "java(post.getCookTime() != null ? post.getCookTime().toString() : null)")
  PostPublishResponse toPublishResponse(Post post);

  //els
  @Mapping(target = "status", expression = "java(post.getStatus() != null ? post.getStatus().name() : null)")
  @Mapping(target = "difficulty", expression = "java(post.getDifficulty() != null ? post.getDifficulty().name() : null)")
  @Mapping(target = "totalReactionCount",
          expression = "java(post.getReactions() != null ? (long) post.getReactions().size() : 0L)")
  @Mapping(target = "totalShares",
          expression = "java(post.getShares() != null ? (long) post.getShares().size() : 0L)")
  PostDocument toDocument(Post post);

  @Mapping(target = "reactionCounts", source = "reactions", qualifiedByName = "mapReactionCounts")
  @Mapping(
    target = "totalShares",
    expression = "java(post.getShares() != null ? (long) post.getShares().size() : 0L)"
  )
  PostResponse toResponse(Post post);

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
