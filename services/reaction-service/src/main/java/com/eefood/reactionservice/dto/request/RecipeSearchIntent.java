package com.eefood.reactionservice.dto.request;

import com.eefood.reactionservice.enums.ChatTool;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecipeSearchIntent {
    private ChatTool tool;
    private String keyword;
    private String difficult;
    private String category;
    private Integer maxCookTime;
    private String weather;
    private String location;
    private Boolean useImage;
}
