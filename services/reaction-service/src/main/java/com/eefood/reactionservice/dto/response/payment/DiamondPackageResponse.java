package com.eefood.reactionservice.dto.response.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiamondPackageResponse {
    private Long id;
    private Long diamondAmount;
    private Long bonusDiamond;
    private BigDecimal price;
    private String currency;
    private Boolean isActive;
}
