package com.eefood.reactionservice.mealplan.model;

import com.eefood.reactionservice.mealplan.enums.MealPlanItemStatus;
import com.eefood.reactionservice.mealplan.enums.MealPlanItemSource;
import com.eefood.reactionservice.mealplan.enums.MealSlot;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meal_plan_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@SuperBuilder
public class MealPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan;

    @Column(name = "plan_date")
    private LocalDate planDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MealSlot mealSlot;

    @Column(name = "item_order")
    private Integer itemOrder;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MealPlanItemSource itemSource;

    private Long recipeId;

    private Long postId;

    @Column(length = 255)
    private String customMealName;

    private Integer plannedServings;

    private Integer actualServings;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MealPlanItemStatus status;

    @Column(length = 255)
    private String recipeTitleSnapshot;

    @Column(length = 500)
    private String imageUrlSnapshot;

    @Column(precision = 10, scale = 2)
    private BigDecimal caloriesPerServingSnapshot;

    @Column(precision = 10, scale = 2)
    private BigDecimal proteinPerServingSnapshot;

    @Column(precision = 10, scale = 2)
    private BigDecimal carbsPerServingSnapshot;

    @Column(precision = 10, scale = 2)
    private BigDecimal fatPerServingSnapshot;

    @Column(precision = 10, scale = 2)
    private BigDecimal sugarPerServingSnapshot;

    @Column(length = 500)
    private String note;

    @OneToMany(mappedBy = "mealPlanItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @SuperBuilder.Default
    private List<MealPlanItemIngredient> ingredients = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
