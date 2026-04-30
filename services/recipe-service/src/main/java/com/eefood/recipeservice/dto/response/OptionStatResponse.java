package com.eefood.recipeservice.dto.response;
import lombok.*;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OptionStatResponse {
    private Long optionId;
    private String content;
    private Long count;
    private Double percent;
}
