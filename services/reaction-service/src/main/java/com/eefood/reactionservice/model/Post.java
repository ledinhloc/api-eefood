package com.eefood.reactionservice.model;

import com.eefood.reactionservice.enums.Difficulty;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "post")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Post extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String content;

  @Column(nullable = false)
  private Long userId;
  // ====== thong tin recipe ===========
  private Long recipeId;
  private String title;
  private String description;
  private String region;
  private String imageUrl;
  private Integer prepTime;
  private Integer cookTime;
  @Enumerated(EnumType.STRING)
  @Column(length = 7)
  private Difficulty difficulty;

  @ElementCollection
  @CollectionTable(name = "post_recipe_categories", joinColumns = @JoinColumn(name = "post_id"))
  @Column(name = "category")
  private Set<String> recipeCategories = new HashSet<>();

  @ElementCollection
  @CollectionTable(name = "post_recipe_ingredient_keywords", joinColumns = @JoinColumn(name = "post_id"))
  @Column(name = "ingredient_keyword")
  private Set<String> recipeIngredientKeywords = new HashSet<>();

  // ====== thong tin cua post ===========
  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Share> shares = new ArrayList<>();

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<PostReaction> reactions = new ArrayList<>();

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Comment> comments = new ArrayList<>();

  //count
  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<PostReactionCount> reactionCounts = new ArrayList<>();

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<CollectionPost> collectionPosts = new ArrayList<>();
}
