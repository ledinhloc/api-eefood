package com.eefood.recipeservice.dto.response;
import lombok.*;

import java.util.List;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionStatResponse {
    private Long questionId;
    private String content;
    private Integer weight;
    private List<OptionStatResponse> options;
}
