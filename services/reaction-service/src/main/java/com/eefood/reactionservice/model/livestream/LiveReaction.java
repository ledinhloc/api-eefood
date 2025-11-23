package com.eefood.reactionservice.model.livestream;
import com.eefood.reactionservice.enums.FoodEmotion;
import com.eefood.reactionservice.enums.ReactionType;
import com.eefood.reactionservice.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "live_reaction")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LiveReaction extends BaseEntity{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FoodEmotion emotion;

  @ManyToOne
  @JoinColumn(name = "live_stream_id", nullable = false)
  private LiveStream liveStream;
}
