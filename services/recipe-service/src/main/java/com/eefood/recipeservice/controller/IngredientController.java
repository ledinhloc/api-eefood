package com.eefood.recipeservice.controller;

import com.eefood.recipeservice.dto.response.IngredientResponse;
import com.eefood.recipeservice.dto.response.ResponseData;
import com.eefood.recipeservice.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping
    public ResponseData<Page<IngredientResponse>> getAllIngredients(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        var result = ingredientService.getAllIngredients(name, pageable);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", result);
    }
}
