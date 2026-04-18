package com.eefood.recipeservice.model;

import com.eefood.recipeservice.enums.CookingSessionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cooking_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CookingSessions extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CookingSessionStatus status;

    @Column(name = "current_step")
    private Integer currentStep = 1;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "cookingSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CookingSessionStep> steps = new HashSet<>();

    public void addStep(CookingSessionStep step) {
        steps.add(step);
        step.setCookingSession(this);
    }

    public void removeStep(CookingSessionStep step) {
        steps.remove(step);
        step.setCookingSession(null);
    }
}
