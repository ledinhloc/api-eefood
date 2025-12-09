package com.eefood.reactionservice.service.report;

import com.eefood.reactionservice.dto.request.ReportRequest;
import com.eefood.reactionservice.dto.response.ReportResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.StoryResponse;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.enums.ReportStatus;
import com.eefood.reactionservice.enums.ReportTargetType;
import com.eefood.reactionservice.mapper.ReportMapper;
import com.eefood.reactionservice.model.Comment;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.model.Story;
import com.eefood.reactionservice.model.report.Report;
import com.eefood.reactionservice.model.report.ReportComment;
import com.eefood.reactionservice.model.report.ReportPost;
import com.eefood.reactionservice.model.report.ReportStory;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.repository.report.ReportCommentRepository;
import com.eefood.reactionservice.repository.report.ReportPostRepository;
import com.eefood.reactionservice.repository.report.ReportStoryRepository;
import com.eefood.reactionservice.util.NotificationUtils;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportPostRepository reportPostRepository;
    private final ReportStoryRepository reportStoryRepository;
    private final ReportCommentRepository reportCommentRepository;
    private final ReportMapper reportMapper;
    private final EntityManager em;
    private final NotificationUtils notificationUtils;
    private final IamClient iamClient;

    public ReportResponse createReport(ReportRequest request) {
        UserInfo data = iamClient.getUserInfo(request.getReporterId()).getData();
        validateRequest(request);

        ReportTargetType type = ReportTargetType.valueOf(request.getTargetType());

        boolean exists = false;

        switch (type) {
            case POST -> exists = reportPostRepository
                    .existsByReporterIdAndPost_Id(request.getReporterId(), request.getTargetId());

            case STORY -> exists = reportStoryRepository
                    .existsByReporterIdAndStory_Id(request.getReporterId(), request.getTargetId());

            case COMMENT -> exists = reportCommentRepository
                    .existsByReporterIdAndComment_Id(request.getReporterId(), request.getTargetId());
        }

        if (exists) {
            throw new IllegalArgumentException("Bạn đã báo cáo nội dung này trước đó");
        }

        Report target;

        switch (type) {
            case POST -> {
                ReportPost rp = ReportPost.builder()
                        .reporterId(request.getReporterId())
                        .reason(request.getReason())
                        .status(ReportStatus.PENDING)
                        .targetType(type)
                        .build();
                rp.setPost(em.getReference(Post.class, request.getTargetId()));
                target = reportPostRepository.save(rp);
            }

            case STORY -> {
                ReportStory rs = ReportStory.builder()
                        .reporterId(request.getReporterId())
                        .reason(request.getReason())
                        .status(ReportStatus.PENDING)
                        .targetType(type)
                        .build();
                rs.setStory(em.getReference(Story.class, request.getTargetId()));
                target = reportStoryRepository.save(rs);
            }

            case COMMENT -> {
                ReportComment rc = ReportComment.builder()
                        .reporterId(request.getReporterId())
                        .reason(request.getReason())
                        .status(ReportStatus.PENDING)
                        .targetType(type)
                        .build();
                rc.setComment(em.getReference(Comment.class, request.getTargetId()));
                target = reportCommentRepository.save(rc);
            }

            default -> throw new IllegalArgumentException("Unsupported targetType");
        }

        notificationUtils.sendReportToAdmin(
                target.getReporterId(),
                request.getReason(),
                request.getTargetId(),
                request.getTargetType(),
                data.getAvatarUrl(),
                request.getImageUrl()
        );

        return mapToResponse(target);
    }


    private void validateRequest(ReportRequest request) {
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new IllegalArgumentException("Reason is required");
        }
        if (request.getReporterId() == null || request.getReporterId() <= 0) {
            throw new IllegalArgumentException("Invalid reporter");
        }
        if (request.getTargetId() == null || request.getTargetId() <= 0) {
            throw new IllegalArgumentException("Target id is required");
        }
    }


    public List<ReportResponse> getUserReports(Long userId) {
        List<ReportResponse> result = new ArrayList<>();

        reportPostRepository.findByReporterId(userId)
                .forEach(r -> result.add(reportMapper.toResponse(r)));

        reportCommentRepository.findByReporterId(userId)
                .forEach(r -> result.add(reportMapper.toResponse(r)));

        reportStoryRepository.findByReporterId(userId)
                .forEach(r -> result.add(reportMapper.toResponse(r)));

        return result;
    }


    public ReportResponse getReportDetail(Long id) {

        ReportResponse data = findReportById(id);
        if (data == null) throw new IllegalArgumentException("Report not found");

        return data;
    }


    private ReportResponse findReportById(Long id) {

        return reportPostRepository.findById(id)
                .map(reportMapper::toResponse)
                .or(() -> reportCommentRepository.findById(id).map(reportMapper::toResponse))
                .or(() -> reportStoryRepository.findById(id).map(reportMapper::toResponse))
                .orElse(null);
    }

    public Page<ReportResponse> getAllReports(Long reporterId, String reason, String status, String type, Pageable pageable) {
        ReportTargetType typeReport = ReportTargetType.valueOf(type);

        if (ReportTargetType.POST.equals(typeReport)) {
            Specification<ReportPost> spec = ReportPostSpecification.isNotDeleted()
                    .and(ReportPostSpecification.hasReporterId(reporterId))
                    .and(ReportPostSpecification.hasReasonLike(reason))
                    .and(ReportPostSpecification.hasStatus(status!=null ? ReportStatus.valueOf(status) : null));
            return reportPostRepository.findAll(spec,pageable)
                    .map(reportMapper::toResponse);
        }
        else if (ReportTargetType.COMMENT.equals(typeReport)) {
            Specification<ReportComment> spec = ReportCommentSpecification.isNotDeleted()
                    .and(ReportCommentSpecification.hasReporterId(reporterId))
                    .and(ReportCommentSpecification.hasReasonLike(reason))
                    .and(ReportCommentSpecification.hasStatus(status!=null ? ReportStatus.valueOf(status) : null));
            return reportCommentRepository.findAll(spec,pageable)
                    .map(reportMapper::toResponse);
        }
        else {
            Specification<ReportStory> spec = ReportStorySpecification.isNotDeleted()
                    .and(ReportStorySpecification.hasReporterId(reporterId))
                    .and(ReportStorySpecification.hasReasonLike(reason))
                    .and(ReportStorySpecification.hasStatus(status!=null ? ReportStatus.valueOf(status) : null));
            return reportStoryRepository.findAll(spec,pageable)
                    .map(reportMapper::toResponse);
        }
    }

    public ReportResponse updateStatus(Long id, ReportTargetType type,ReportStatus status) {

        Report updated = null;

        if (type.equals(ReportTargetType.POST)) {
            ReportPost rp = reportPostRepository.findById(id).orElseThrow();
            rp.setStatus(status);
            updated = reportPostRepository.save(rp);
        } else if (type.equals(ReportTargetType.STORY)) {
            ReportStory rs = reportStoryRepository.findById(id).orElseThrow();
            rs.setStatus(status);
            updated = reportStoryRepository.save(rs);
        } else if (type.equals(ReportTargetType.COMMENT)) {
            ReportComment rc = reportCommentRepository.findById(id).orElseThrow();
            rc.setStatus(status);
            updated = reportCommentRepository.save(rc);
        } else {
            throw new IllegalArgumentException("Report not found");
        }

        return mapToResponse(updated);
    }

    private ReportResponse mapToResponse(Report entity) {

        if (entity instanceof ReportPost rp) {
            return reportMapper.toResponse(rp);
        } else if (entity instanceof ReportStory rs) {
            return reportMapper.toResponse(rs);
        } else {
            return reportMapper.toResponse((ReportComment) entity);
        }
    }
}

