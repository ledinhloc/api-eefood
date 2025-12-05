package com.eefood.reactionservice.dto.request;

import lombok.Data;

@Data
public class StoryCommentRequest {
    private Long id;
    private Long storyId;
    private String message;
    private Long parentId;
}
