package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.response.CommentReactionResponse;
import com.eefood.reactionservice.model.CommentReaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentReactionMapper {
    @Mapping(target = "commentId", source = "comment.id")
    CommentReactionResponse toResponse(CommentReaction commentReaction);
}
