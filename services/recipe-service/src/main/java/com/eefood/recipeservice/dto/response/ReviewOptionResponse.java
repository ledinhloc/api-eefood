package com.eefood.recipeservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewOptionResponse {
    private Long id;
    private String content;
    private Integer starValue;
}
