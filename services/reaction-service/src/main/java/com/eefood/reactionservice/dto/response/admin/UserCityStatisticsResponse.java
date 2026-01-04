package com.eefood.reactionservice.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserCityStatisticsResponse {
    private String city;
    private Long totalUsers;
}
