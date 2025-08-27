package com.eefood.iamservice.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name = "otp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Otp {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // liên kết tới User
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private String otpNum;

  @Column(nullable = false)
  private LocalDateTime otpExpired;

  @CreatedDate
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private Boolean isDeleted = false;
}
