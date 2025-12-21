package com.eefood.recipeservice;

import com.eefood.recipeservice.dto.ShoppingItemDto;
import com.eefood.recipeservice.mapper.ShoppingListMapper;
import com.eefood.recipeservice.model.ShoppingIngredient;
import com.eefood.recipeservice.model.ShoppingItem;
import com.eefood.recipeservice.repository.ShoppingItemRepository;
import com.eefood.recipeservice.service.ShoppingListService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {

  @InjectMocks
  private ShoppingListService service;

  @Mock
  private ShoppingItemRepository itemRepo;

  @Mock
  private ShoppingListMapper mapper;

  // ===== Test 1: newServings invalid =====
  @Test
  void updateServings_invalid_throwBadRequest() {
    Long userId = 1L;
    Long itemId = 2L;

    assertThrows(RuntimeException.class, () ->
      service.updateServings(userId, itemId, 0)
    );

    verifyNoInteractions(itemRepo, mapper);
  }

  // ===== Test 2: item not found =====
  @Test
  void updateServings_itemNotFound_throwNotFound() {
    Long userId = 1L;
    Long itemId = 2L;

    when(itemRepo.findByIdAndUserIdAndIsDeletedFalse(itemId, userId))
      .thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () ->
      service.updateServings(userId, itemId, 3)
    );

    verify(itemRepo).findByIdAndUserIdAndIsDeletedFalse(itemId, userId);
    verify(itemRepo, never()).save(any());
    verifyNoInteractions(mapper);
  }

  // ===== Test 3: update success =====
  @Test
  void updateServings_success_updateQuantitiesAndReturnDto() {
    Long userId = 1L;
    Long itemId = 2L;

    // old servings = 2
    ShoppingIngredient ing1 = new ShoppingIngredient();
    ing1.setQuantity(200.0); // 100 per serving

    ShoppingIngredient ing2 = new ShoppingIngredient();
    ing2.setQuantity(100.0); // 50 per serving

    ShoppingItem item = new ShoppingItem();
    item.setId(itemId);
    item.setServings(2);
    item.setIngredients(List.of(ing1, ing2));

    when(itemRepo.findByIdAndUserIdAndIsDeletedFalse(itemId, userId))
      .thenReturn(Optional.of(item));

    ShoppingItemDto dto = ShoppingItemDto.builder()
      .id(itemId)
      .servings(4)
      .build();

    when(mapper.toDto(any(ShoppingItem.class))).thenReturn(dto);

    // update to newServings = 4
    ShoppingItemDto result = service.updateServings(userId, itemId, 4);

    // ===== verify logic =====
    assertEquals(4, item.getServings());
    assertEquals(400.0, ing1.getQuantity()); // 100 * 4
    assertEquals(200.0, ing2.getQuantity()); // 50 * 4

    assertNotNull(result);
    assertEquals(4, result.getServings());

    verify(itemRepo).save(item);
    verify(mapper).toDto(item);
  }
}
