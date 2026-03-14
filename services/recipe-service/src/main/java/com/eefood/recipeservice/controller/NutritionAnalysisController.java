package com.eefood.recipeservice.controller;

import com.eefood.recipeservice.dto.request.ImageAnalysisRequest;
import com.eefood.recipeservice.dto.response.NutritionAnalysisResponse;
import com.eefood.recipeservice.dto.response.ResponseData;
import com.eefood.recipeservice.service.nutrition.NutritionAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/nutrition")
@RequiredArgsConstructor
public class NutritionAnalysisController {
    private final NutritionAnalysisService nutritionAnalysisService;

    //Phân tích dinh dưỡng từ recipeId
    @PostMapping(value = "/recipe/{recipeId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeStreamByRecipeId(@PathVariable Long recipeId,  @RequestParam(defaultValue = "false") boolean forceRefresh)
    {
        return nutritionAnalysisService.analyzeStreamByRecipeId(recipeId, forceRefresh);
    }

    // Phân tích dinh dưỡng từ ảnh (base64)
    @PostMapping( value = "/image/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeStreamByImage(@RequestBody ImageAnalysisRequest request)
    {
        return nutritionAnalysisService.analyzeStreamByImage(request.getImageUrl());
    }
}
