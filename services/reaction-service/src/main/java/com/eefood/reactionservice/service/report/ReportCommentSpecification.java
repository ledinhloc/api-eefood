package com.eefood.reactionservice.service.report;

import com.eefood.reactionservice.enums.ReportStatus;
import com.eefood.reactionservice.model.report.ReportComment;
import com.eefood.reactionservice.model.report.ReportPost;
import org.springframework.data.jpa.domain.Specification;

public class ReportCommentSpecification {
    public ReportCommentSpecification() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Specification<ReportComment> hasReporterId(Long reporterId) {
        return (root, query, cb) -> {
            if (reporterId == null) return null;
            return cb.equal(root.get("reporterId"), reporterId);
        };
    }

    public static Specification<ReportComment> hasReasonLike(String reason) {
        return (root, query, cb)->{
            if(reason == null || reason.isBlank()) return null;
            return cb.like(cb.lower(root.get("reason")), "%" + reason.toLowerCase() + "%");
        };
    }

    public static Specification<ReportComment> hasStatus(ReportStatus status) {
        return (root, query, cb) ->{
            if(status == null) return null;
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<ReportComment> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }
}
