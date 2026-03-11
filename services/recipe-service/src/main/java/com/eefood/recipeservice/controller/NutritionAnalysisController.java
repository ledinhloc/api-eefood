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

@RestController
@RequestMapping("/api/v1/nutrition")
@RequiredArgsConstructor
public class NutritionAnalysisController {
    private final NutritionAnalysisService nutritionAnalysisService;

    //Phân tích dinh dưỡng từ recipeId
    @PostMapping("/recipe/{recipeId}")
    public ResponseData<NutritionAnalysisResponse> analyzeByRecipeId(@PathVariable Long recipeId) {
        NutritionAnalysisResponse result = nutritionAnalysisService.analyzeByRecipeId(recipeId);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", result);
    }

    // Phân tích dinh dưỡng từ ảnh (base64)
    @PostMapping( "/image")
    public ResponseData<NutritionAnalysisResponse> analyzeByImage(@RequestBody ImageAnalysisRequest request) {
        NutritionAnalysisResponse result = nutritionAnalysisService.analyzeByImage(request.getBase64Image());
        return new ResponseData<>(HttpStatus.OK.value(), "Success", result);
    }
}
