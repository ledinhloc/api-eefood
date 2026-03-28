package com.eefood.reactionservice.mealplan.model;

import com.eefood.reactionservice.mealplan.enums.MealPlanItemStatus;
import com.eefood.reactionservice.mealplan.enums.MealPlanItemSource;
import com.eefood.reactionservice.mealplan.enums.MealSlot;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Column(name = "meal_plan_id", nullable = false)
    private Long mealPlanId;

    @Column(name = "plan_date")
    private LocalDate planDate;

    @Enumerated(EnumType.STRING)
    @Column
    private MealSlot mealSlot;

    @Column(name = "item_order")
    private Integer itemOrder;

    @Enumerated(EnumType.STRING)
    @Column
    private MealPlanItemSource itemSource;

    private Long recipeId;

    private Long postId;

    @Column
    private String customMealName;

    private Integer plannedServings;

    private Integer actualServings;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MealPlanItemStatus status;

    @Column
    private String recipeTitle;

    @Column(length = 500)
    private String imageUrl;

    @Column(precision = 10, scale = 2)
    private BigDecimal calories;

    @Column(precision = 10, scale = 2)
    private BigDecimal protein;

    @Column(precision = 10, scale = 2)
    private BigDecimal carbs;

    @Column(precision = 10, scale = 2)
    private BigDecimal fat;

    @Column(precision = 10, scale = 2)
    private BigDecimal fiber;

    @Column(precision = 10, scale = 2)
    private BigDecimal sugar;

    @Column(precision = 10, scale = 2)
    private BigDecimal calcium;

    @Column(precision = 10, scale = 2)
    private BigDecimal sodium;

    @Column(length = 500)
    private String note;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
