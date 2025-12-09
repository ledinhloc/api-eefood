package com.eefood.reactionservice.service.report;

import com.eefood.reactionservice.enums.ReportStatus;
import com.eefood.reactionservice.model.report.ReportComment;
import com.eefood.reactionservice.model.report.ReportStory;
import org.springframework.data.jpa.domain.Specification;

public class ReportStorySpecification {
    public ReportStorySpecification() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Specification<ReportStory> hasReporterId(Long reporterId) {
        return (root, query, cb) -> {
            if (reporterId == null) return null;
            return cb.equal(root.get("reporterId"), reporterId);
        };
    }

    public static Specification<ReportStory> hasReasonLike(String reason) {
        return (root, query, cb)->{
            if(reason == null || reason.isBlank()) return null;
            return cb.like(cb.lower(root.get("reason")), "%" + reason.toLowerCase() + "%");
        };
    }

    public static Specification<ReportStory> hasStatus(ReportStatus status) {
        return (root, query, cb) ->{
            if(status == null) return null;
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<ReportStory> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }
}
