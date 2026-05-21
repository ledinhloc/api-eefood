package com.eefood.recipeservice.controller;

import com.eefood.recipeservice.dto.response.NutritionAnalysisResponse;
import com.eefood.recipeservice.dto.response.ResponseData;
import com.eefood.recipeservice.service.nutrition.NutritionAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/nutrition")
@RequiredArgsConstructor
public class NutritionAnalysisController {
    private final NutritionAnalysisService nutritionAnalysisService;

    // Phan tich dinh duong dang JSON, khong dung SSE cho chatbot no auth
    @GetMapping("/recipe/{recipeId}/chatbot")
    public ResponseData<NutritionAnalysisResponse> getNutritionByRecipeIdForChatbot(
            @PathVariable Long recipeId,
            @RequestParam(defaultValue = "false") boolean forceRefresh
    ) {
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Get Nutrition By Recipe Success",
                nutritionAnalysisService.getNutritionByRecipeIdFull(recipeId, forceRefresh)
        );
    }

    // Phan tich dinh duong dang JSON, khong dung SSE.
    @GetMapping("/recipe/{recipeId}")
    public ResponseData<NutritionAnalysisResponse> getNutritionByRecipeId(
            @PathVariable Long recipeId,
            @RequestParam(defaultValue = "false") boolean forceRefresh
    ) {
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Get Nutrition By Recipe Success",
                nutritionAnalysisService.getNutritionByRecipeId(recipeId, forceRefresh)
        );
    }

    // Phan tich dinh duong tu recipeId bang SSE.
    @PostMapping(value = "/recipe/{recipeId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeStreamByRecipeId(@PathVariable Long recipeId,  @RequestParam(defaultValue = "false") boolean forceRefresh)
    {
        return nutritionAnalysisService.analyzeStreamByRecipeId(recipeId, forceRefresh);
    }

    // Phân tích dinh dưỡng từ ảnh (base64)
    @PostMapping( value = "/image/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SseEmitter analyzeStreamByImage(@RequestParam MultipartFile image)
    {
        return nutritionAnalysisService.analyzeStreamByImage(image);
    }
}
