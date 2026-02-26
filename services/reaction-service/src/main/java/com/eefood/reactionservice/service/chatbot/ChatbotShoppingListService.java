package com.eefood.reactionservice.service.chatbot;

import com.eefood.reactionservice.dto.response.ShoppingItemDto;
import com.eefood.reactionservice.repository.httpclient.RecipeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotShoppingListService {

    private final RecipeClient recipeClient;

    public ShoppingItemDto addItem(Long recipeId, Long userId) {
        return recipeClient.addRecipe(recipeId,userId,1).getData();
    }
}
