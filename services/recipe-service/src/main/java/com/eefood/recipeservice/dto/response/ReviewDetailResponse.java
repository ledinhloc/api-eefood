package com.eefood.recipeservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewDetailResponse {
    private Long reviewId;
    private Long userId;
    private String name;
    private String avatar;
    private Double rating;
    private LocalDateTime createdAt;
    private List<String> tags;
}
