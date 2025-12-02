package com.eefood.reactionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryCollectionResponse {
    private Long id;
    private Long userId;
    private String name;
    private String imageUrl;
    private String description;
}
