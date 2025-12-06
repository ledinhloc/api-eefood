package com.eefood.reactionservice.service.report;

import com.eefood.reactionservice.dto.request.ReportRequest;
import com.eefood.reactionservice.dto.response.ReportResponse;
import com.eefood.reactionservice.dto.response.StoryResponse;
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
import com.eefood.reactionservice.repository.report.ReportCommentRepository;
import com.eefood.reactionservice.repository.report.ReportPostRepository;
import com.eefood.reactionservice.repository.report.ReportStoryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public ReportResponse createReport(ReportRequest request) {

        validateRequest(request);

        Report target;

        switch (request.getTargetType()) {
            case POST -> {
                ReportPost rp = ReportPost.builder()
                        .reporterId(request.getReporterId())
                        .reason(request.getReason())
                        .status(ReportStatus.PENDING)
                        .targetType(request.getTargetType())
                        .build();
                rp.setPost(em.getReference(Post.class, request.getTargetId()));
                target = reportPostRepository.save(rp);
            }
            case STORY -> {
                ReportStory rs = ReportStory.builder()
                        .reporterId(request.getReporterId())
                        .reason(request.getReason())
                        .status(ReportStatus.PENDING)
                        .targetType(request.getTargetType())
                        .build();
                rs.setStory(em.getReference(Story.class, request.getTargetId()));
                target = reportStoryRepository.save(rs);
            }
            case COMMENT -> {
                ReportComment rc = ReportComment.builder()
                        .reporterId(request.getReporterId())
                        .reason(request.getReason())
                        .status(ReportStatus.PENDING)
                        .targetType(request.getTargetType())
                        .build();
                rc.setComment(em.getReference(Comment.class, request.getTargetId()));
                target = reportCommentRepository.save(rc);
            }
            default -> throw new IllegalArgumentException("Unsupported targetType");
        }

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

    public Page<ReportResponse> getAllReports(String type, Pageable pageable) {
        ReportTargetType typeReport = ReportTargetType.valueOf(type);

        if (ReportTargetType.POST.equals(typeReport)) {
            return reportPostRepository.findAll(pageable)
                    .map(reportMapper::toResponse);
        }
        else if (ReportTargetType.COMMENT.equals(typeReport)) {
            return reportCommentRepository.findAll(pageable)
                    .map(reportMapper::toResponse);
        }
        else {
            return reportStoryRepository.findAll(pageable)
                    .map(reportMapper::toResponse);
        }
    }

    public ReportResponse updateStatus(Long id, ReportStatus status) {

        Report updated = null;

        if (reportPostRepository.existsById(id)) {
            ReportPost rp = reportPostRepository.findById(id).orElseThrow();
            rp.setStatus(status);
            updated = reportPostRepository.save(rp);
        } else if (reportStoryRepository.existsById(id)) {
            ReportStory rs = reportStoryRepository.findById(id).orElseThrow();
            rs.setStatus(status);
            updated = reportStoryRepository.save(rs);
        } else if (reportCommentRepository.existsById(id)) {
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

