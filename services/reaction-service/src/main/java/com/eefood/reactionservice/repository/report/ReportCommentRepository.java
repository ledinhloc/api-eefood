package com.eefood.reactionservice.repository.report;

import com.eefood.reactionservice.model.report.ReportComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportCommentRepository extends JpaRepository<ReportComment, Long> {
    List<ReportComment> findByReporterId(Long reporterId);
}
