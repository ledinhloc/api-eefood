package com.eefood.reactionservice.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "approve_post")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
public class ApprovePost {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  private Long recipeId;
  private Long userId;

  private String status;
  private String summary;
  private Double totalScore;

  private Integer recipeCompleteness;
  private Integer ingredientSafety;
  private Integer stepClarity;
  private Integer contentAppropriate;
  private Integer contentRelevance;
  private Integer mediaQuality;

  private String completenessNote;
  private String safetyNote;
  private String clarityNote;
  private String appropriatenessNote;
  private String relevanceNote;
  private String mediaQualityNote;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
