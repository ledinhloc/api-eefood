package com.eefood.reactionservice.dto.response.admin;

import com.eefood.reactionservice.dto.response.UserResponse;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatistics {
    private Long totalUsers;
    private List<TopUserResponse> topInfluencers; // Top 3 người dùng có nhiều follower
    private List<UserRegistrationStatsResponse> recentRegistrations; // So nguoi dang ky theo ngay
    private List<TopUserPostResponse> topPostCreators; // Top người dùng đăng bài nhiều
    private List<UserCityStatisticsResponse> cityStatistics;
}
