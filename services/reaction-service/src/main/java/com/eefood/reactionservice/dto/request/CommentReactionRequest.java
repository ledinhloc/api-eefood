package com.eefood.reactionservice.dto.request;

import com.eefood.reactionservice.enums.ReactionType;
import lombok.Data;

@Data
public class CommentReactionRequest {
    private Long commentId;
    private ReactionType reactionType;
}
