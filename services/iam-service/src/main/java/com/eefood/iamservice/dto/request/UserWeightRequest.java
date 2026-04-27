package com.eefood.iamservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class UserWeightRequest {
  @NotNull(message = "Weight is required")
  @DecimalMin(value = "0.01", message = "Weight must be greater than 0")
  private BigDecimal weightKg;

  private LocalDate recordedDate;
}
