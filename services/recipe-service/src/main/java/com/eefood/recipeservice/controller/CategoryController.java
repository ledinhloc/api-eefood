package com.eefood.recipeservice.controller;

import com.eefood.recipeservice.dto.response.CategoryResponse;
import com.eefood.recipeservice.dto.response.ResponseData;
import com.eefood.recipeservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseData<Page<CategoryResponse>> getAllCategories(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    )
    {
        Pageable pageable = PageRequest.of(page - 1, limit);
        var result = categoryService.getAllCategories(name, pageable);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", result);
    }
}
