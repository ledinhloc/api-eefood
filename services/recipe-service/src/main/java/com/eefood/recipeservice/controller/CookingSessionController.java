package com.eefood.recipeservice.controller;

import com.eefood.recipeservice.dto.request.CookingSessionProgressRequest;
import com.eefood.recipeservice.dto.response.CookingSessionProgressResponse;
import com.eefood.recipeservice.dto.response.CookingSessionResponse;
import com.eefood.recipeservice.dto.response.ResponseData;
import com.eefood.recipeservice.service.CookingSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/cooking")
@RequiredArgsConstructor
public class CookingSessionController {
    private final CookingSessionService cookingSessionService;

    @GetMapping("/recipe/{recipeId}")
    public ResponseData<CookingSessionResponse> getOrCreateCookingSession(@PathVariable Long recipeId) {
        CookingSessionResponse response = cookingSessionService.getOrCreateCookingSession(recipeId);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", response);
    }

    @PutMapping("/{sessionId}/progress")
    public ResponseData<CookingSessionProgressResponse> saveProgress(@PathVariable Long sessionId, @RequestBody @Valid CookingSessionProgressRequest request) {
        CookingSessionProgressResponse response = cookingSessionService.savePartProgress(sessionId, request);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", response);
    }

    @PutMapping("/{sessionId}/complete")
    public ResponseData<CookingSessionProgressResponse> completeSession(@PathVariable Long sessionId) {
        CookingSessionProgressResponse response = cookingSessionService.completeSession(sessionId);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", response);
    }
}
