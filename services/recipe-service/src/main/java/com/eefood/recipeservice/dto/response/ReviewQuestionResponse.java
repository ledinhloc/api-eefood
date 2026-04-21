package com.eefood.recipeservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewQuestionResponse {
    private Long id;
    private String content;
    private Integer weight;
    private Boolean isActive;
    private List<ReviewOptionResponse> options;
}
