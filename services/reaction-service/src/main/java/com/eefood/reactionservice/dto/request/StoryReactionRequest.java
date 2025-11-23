package com.eefood.reactionservice.dto.request;

import com.eefood.reactionservice.enums.ReactionType;
import lombok.Data;

@Data
public class StoryReactionRequest {
    private Long storyId;
    private ReactionType reactionType;
}
