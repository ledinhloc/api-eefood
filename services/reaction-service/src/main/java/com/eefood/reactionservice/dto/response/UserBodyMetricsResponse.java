package com.eefood.reactionservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBodyMetricsResponse {
  private Long userId;
  private BigDecimal heightCm;
  private LocalDate heightRecordedDate;
  private BigDecimal weightKg;
  private LocalDate weightRecordedDate;
}
