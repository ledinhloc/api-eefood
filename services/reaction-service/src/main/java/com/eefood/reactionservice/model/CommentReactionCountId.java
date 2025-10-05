package com.eefood.reactionservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentReactionCountId implements Serializable {
  private Long comment; // trùng tên field trong entity (Comment comment)
  private String reactionType;
}