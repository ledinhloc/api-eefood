package com.eefood.reactionservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoryCollectionRequest {
    private String name;
    private String imageUrl;
    private String description;
}
