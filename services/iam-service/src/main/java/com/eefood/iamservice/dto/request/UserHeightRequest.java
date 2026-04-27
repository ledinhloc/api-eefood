package com.eefood.iamservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class UserHeightRequest {
  @NotNull(message = "Height is required")
  @DecimalMin(value = "0.01", message = "Height must be greater than 0")
  private BigDecimal heightCm;

  private LocalDateTime recordedAt;
}
