package com.eefood.reactionservice.service.admin;

import com.eefood.reactionservice.dto.request.UserNotificationResquest;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.dto.response.UserResponse;
import com.eefood.reactionservice.dto.response.admin.*;
import com.eefood.reactionservice.enums.PostStatus;
import com.eefood.reactionservice.enums.ReactionType;
import com.eefood.reactionservice.enums.ReportStatus;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.model.report.Report;
import com.eefood.reactionservice.model.report.ReportPost;
import com.eefood.reactionservice.repository.FollowRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.repository.post.PostReactionCountRepository;
import com.eefood.reactionservice.repository.post.PostReactionRepository;
import com.eefood.reactionservice.repository.post.PostRepository;
import com.eefood.reactionservice.repository.report.ReportPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final FollowRepository followRepository;
    private final PostRepository postRepository;
    private final PostReactionCountRepository postReactionCountRepository;
    private final ReportPostRepository reportPostRepository;
    private final IamClient iamClient;

    public List<UserCityStatisticsResponse> getUserStatisticsByCity() {
        var response = iamClient.getUserStatisticsByCity();
        return response != null ? response.getData() : List.of();
    }

    public PostStatistics getPostStatistics(
            int topPostsLimit,
            int recentViolatedPostsLimit
    ) {
        // Top bài viết có nhiều lượt like
        List<TopPostResponse> topLikedPosts = getTopLikedPosts(topPostsLimit);

        // Tổng số bài viết vi phạm
        Long totalViolatedPosts = countViolatedPosts();

        // Danh sách bài viết vi phạm gần đây
        List<ViolatedPostResponse> recentViolatedPosts = getRecentViolatedPosts(recentViolatedPostsLimit);

        return PostStatistics.builder()
                .topLikedPosts(topLikedPosts)
                .totalViolatedPosts(totalViolatedPosts)
                .recentViolatedPosts(recentViolatedPosts)
                .build();
    }

    public UserStatistics getUserStatistics(
            int topInfluencersLimit,
            int topPostCreatorsLimit
    ) {
        // Tổng user
        Long totalUsers = countTotalUser();
        // Top người nổi tiếng (nhiều lượt theo dõi)
        List<TopUserResponse> topInfluencers = getTopInfluencers(topInfluencersLimit);
        // Danh sách những người đăng ký gần đây
        List<UserRegistrationStatsResponse> recentRegistrations = getRecentRegistrations();
        // Top người đóng góp nhiều nhất
        List<TopUserPostResponse> topPostCreators = getTopPostCreators(topPostCreatorsLimit);
        // Thống kê người dùng theo khu vực
        List<UserCityStatisticsResponse> cityStatisticsResponses = getUserStatisticsByCity();

        return UserStatistics.builder()
                .totalUsers(totalUsers)
                .topInfluencers(topInfluencers)
                .recentRegistrations(recentRegistrations)
                .topPostCreators(topPostCreators)
                .cityStatistics(cityStatisticsResponses)
                .build();
    }

    public Long countTotalUser() {
        var response = iamClient.getAllUserNotifications();
        List<UserNotificationResquest> users = response.getData();
        return users.stream().count();
    }

    public List<TopUserResponse> getTopInfluencers(int limit) {
        List<Object[]> results = followRepository.findTopUsersByFollowerCount(PageRequest.of(0, limit));

        List<Long> userIds = results.stream()
                .map(row -> (Long) row[0])
                .collect(Collectors.toList());

        Map<Long, UserInfo> userInfoMap = fetchUserInfoMap(userIds);

        return results.stream()
                .map(row -> {
                    Long userId = (Long) row[0];
                    Long followerCount = (Long) row[1];
                    return TopUserResponse.builder()
                            .userInfo(userInfoMap.get(userId))
                            .followerCount(followerCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<UserRegistrationStatsResponse> getRecentRegistrations() {
        var response = iamClient.getRecentUsers();
        List<UserRegistrationStatsResponse> data = response.getData();
        return data;
    }

    private List<TopUserPostResponse> getTopPostCreators(int limit) {
        List<Object[]> results = postRepository.findTopUsersByPostCount(PageRequest.of(0, limit));

        List<Long> userIds = results.stream()
                .map(row -> (Long) row[0])
                .collect(Collectors.toList());

        Map<Long, UserInfo> userInfoMap = fetchUserInfoMap(userIds);

        return results.stream()
                .map(row -> {
                    Long userId = (Long) row[0];
                    Long postCount = (Long) row[1];
                    return TopUserPostResponse.builder()
                            .userInfo(userInfoMap.get(userId))
                            .postCount(postCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private Map<Long, UserInfo> fetchUserInfoMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        try {
            ResponseData<List<UserInfo>> response = iamClient.getUserInfoBatch(userIds);
            if (response != null && response.getData() != null) {
                return response.getData().stream()
                        .collect(Collectors.toMap(UserInfo::getId, u -> u));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return new HashMap<>();
    }

    private List<TopPostResponse> getTopLikedPosts(int limit) {

        List<Object[]> results =
                postReactionCountRepository.findTopPostsByReactionTypes(
                        List.of(ReactionType.LIKE, ReactionType.LOVE),
                        PageRequest.of(0, limit)
                );

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> postIds = results.stream()
                .map(r -> (Long) r[0])
                .toList();

        Map<Long, Post> postMap = postRepository.findAllById(postIds)
                .stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        List<Long> userIds = postMap.values()
                .stream()
                .map(Post::getUserId)
                .distinct()
                .toList();

        Map<Long, UserInfo> userInfoMap = fetchUserInfoMap(userIds);

        return results.stream()
                .map(row -> {
                    Long postId = (Long) row[0];
                    Long totalLikeLove = (Long) row[1];
                    Post post = postMap.get(postId);

                    return TopPostResponse.builder()
                            .postId(postId)
                            .title(post.getTitle())
                            .imageUrl(post.getImageUrl())
                            .count(totalLikeLove)
                            .userInfo(userInfoMap.get(post.getUserId()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private Long countViolatedPosts() {
        return postRepository.countByStatus(PostStatus.REJECTED);
    }

    private List<ViolatedPostResponse> getRecentViolatedPosts(int limit) {
        List<Post> violatedPosts = postRepository.findByStatusAndIsDeletedFalse(
                PostStatus.REJECTED,
                PageRequest.of(0, limit)
        );

        return violatedPosts.stream()
                .map(post -> {
                    Optional<ReportPost> resolvedReport =
                            reportPostRepository.findFirstByPostIdAndStatus(
                                    post.getId(),
                                    ReportStatus.RESOLVED
                            );
                    return ViolatedPostResponse.builder()
                            .postId(post.getId())
                            .title(post.getTitle())
                            .content(post.getContent())
                            .userId(post.getUserId())
                            .username(getUsernameFromUserInfo(post.getUserId()))
                            .reason(resolvedReport
                                    .map(Report::getReason)
                                    .orElse("Chưa có report được xử lý"))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String getUsernameFromUserInfo(Long userId) {
        try {
            ResponseData<UserInfo> response = iamClient.getUserInfo(userId);
            if (response != null && response.getData() != null) {
                return response.getData().getUsername();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return "Unknown";
    }

}
