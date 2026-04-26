package com.eefood.iamservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserHeightResponse {
  private Long id;
  private Long userId;
  private BigDecimal heightCm;
  private LocalDateTime recordedAt;
}
