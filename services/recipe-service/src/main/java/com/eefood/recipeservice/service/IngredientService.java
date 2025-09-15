package com.eefood.recipeservice.service;

import com.eefood.recipeservice.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class IngredientService {
    private final IngredientRepository ingredientRepository;
}
