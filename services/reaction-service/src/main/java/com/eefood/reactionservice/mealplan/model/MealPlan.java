package com.eefood.reactionservice.mealplan.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "meal_plan",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_meal_plan_user_id", columnNames = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@SuperBuilder
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 255)
    private String goal;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(length = 500)
    private String note;

    @Column(length = 500)
    private String userHealthNote;

    @OneToMany(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("planDate ASC, mealSlot ASC")
    @SuperBuilder.Default
    private List<MealPlanItem> items = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
