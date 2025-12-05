package com.eefood.reactionservice.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.ArrayList;
import java.util.List;

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

  @Column(nullable = false)
  private Long userId;

  private Long recipeId;

  private String title;

  private String content;

  @Column(name = "image_url")
  private String imageUrl;

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
