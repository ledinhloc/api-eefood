package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.response.CommentResponse;
import com.eefood.reactionservice.model.Comment;
import com.eefood.reactionservice.model.CommentReaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "reactionCounts", source = "reactions", qualifiedByName = "mapReactionCounts")
    @Mapping(target = "replies", ignore = true)
    @Mapping(target = "replyCount", ignore = true)
    @Mapping(target = "images", source = "images")
    @Mapping(target = "videos", source = "videos")
    @Mapping(target = "parentId", source = "parent.id")
    CommentResponse toResponse(Comment comment);

    @Named("mapReactionCounts")
    default Map<String, Long> mapReactionCounts(List<CommentReaction> reactions) {
        if (reactions == null) return Map.of();
        return reactions.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getReactionType().name(),
                        Collectors.counting()
                ));
    }
}
