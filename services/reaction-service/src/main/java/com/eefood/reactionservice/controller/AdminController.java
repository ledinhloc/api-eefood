package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.request.UpdateReportRequest;
import com.eefood.reactionservice.dto.response.PostResponse;
import com.eefood.reactionservice.dto.response.ReportResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.admin.PostStatistics;
import com.eefood.reactionservice.dto.response.admin.UserStatistics;
import com.eefood.reactionservice.enums.ReportStatus;
import com.eefood.reactionservice.enums.ReportTargetType;
import com.eefood.reactionservice.service.admin.DashboardService;
import com.eefood.reactionservice.service.chatbot.ChromaEmbeddingService;
import com.eefood.reactionservice.service.post.PostService;
import com.eefood.reactionservice.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final PostService postService;
    private final ReportService reportService;
    private final DashboardService dashboardService;
    private final ChromaEmbeddingService chromaEmbeddingService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/chroma/posts/sync")
    public ResponseData<Map<String, Long>> syncApprovedPostsToChroma() {
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Approved posts synced to ChromaDB",
                chromaEmbeddingService.syncApprovedPostsToChroma()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/posts/statistics")
    public ResponseData<PostStatistics> getPostStatistics(
            @RequestParam(defaultValue = "10") int topPostsLimit,
            @RequestParam(defaultValue = "5") int recentViolatedPostsLimit
    ) {
        PostStatistics postStats = dashboardService.getPostStatistics(
                topPostsLimit,
                recentViolatedPostsLimit
        );

        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Post statistics retrieved successfully",
                postStats
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/statistics")
    public ResponseData<UserStatistics> getUserStatistics(
            @RequestParam(defaultValue = "3") int topInfluencersLimit,
            @RequestParam(defaultValue = "5") int recentRegistrationsLimit,
            @RequestParam(defaultValue = "5") int topPostCreatorsLimit
    ) {
        UserStatistics userStats = dashboardService.getUserStatistics(
                topInfluencersLimit,
                topPostCreatorsLimit
        );

        return new ResponseData<>(
                HttpStatus.OK.value(),
                "User statistics retrieved successfully",
                userStats
        );
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reports")
    public ResponseData<Page<ReportResponse>> listAll(
            @RequestParam(required = false) Long reporterId,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "POST") String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, sortBy));
        return new ResponseData<>(200, "Success", reportService.getAllReports(reporterId, reason, status,type, pageable));
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/reports/{id}")
    public ResponseData<ReportResponse> updateStatus(@PathVariable Long id, @RequestBody UpdateReportRequest request) {
        return new ResponseData<>(200, "Update successfully",
                reportService.updateStatus(id, ReportTargetType.valueOf(request.getType()), ReportStatus.valueOf(request.getStatus())));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/posts")
    public ResponseData<Page<PostResponse>> getAllPostsByAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer minPrepTime,
            @RequestParam(required = false) Integer maxPrepTime,
            @RequestParam(required = false) Integer minCookTime,
            @RequestParam(required = false) Integer maxCookTime,
            @RequestParam(required = false) Integer minReactionCount,
            @RequestParam(required = false) Integer minTotalShares,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<PostResponse> result = postService.getAllPostsByAdmin(
                keyword, userId, region, difficulty, category,
                minPrepTime, maxPrepTime, minCookTime, maxCookTime,
                minReactionCount, minTotalShares,
                status, sortBy, pageable
        );

        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Success",
                result
        );
    }
}
