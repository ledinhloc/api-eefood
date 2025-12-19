package com.eefood.iamservice.dto.response;

import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRegistrationStatsResponse {
    private LocalDate date;
    private Long totalUsers;
}
