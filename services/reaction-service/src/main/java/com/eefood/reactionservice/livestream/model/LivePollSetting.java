package com.eefood.reactionservice.livestream.model;

import com.eefood.reactionservice.livestream.enums.PollOptionAddMode;
import com.eefood.reactionservice.livestream.enums.PollResultVisibility;
import com.eefood.reactionservice.livestream.enums.PollVoterVisibility;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
  name = "live_poll_settings",
  indexes = @Index(name="idx_poll_settings_poll", columnList="pollId"),
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_poll_setting_poll", columnNames = "pollId")
  }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LivePollSetting {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long pollId;

  @Column(nullable = false)
  @Builder.Default
  private Boolean allowChangeVote = false;

  @Column(nullable = false)
  private Boolean multipleChoice = false;

  @Column(nullable = false)
  private Integer maxChoices;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PollResultVisibility resultVisibility = PollResultVisibility.AFTER_VOTE;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PollVoterVisibility voterVisibility = PollVoterVisibility.PUBLIC;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PollOptionAddMode optionAddMode = PollOptionAddMode.HOST_ONLY;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}