package com.eefood.iamservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "user_height")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserHeight {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, precision = 5, scale = 2)
  private BigDecimal heightCm;

  @Column(nullable = false)
  private LocalDateTime recordedAt;
}
