package com.eefood.reactionservice.repository.report;

import com.eefood.reactionservice.model.report.ReportComment;
import com.eefood.reactionservice.model.report.ReportPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportCommentRepository extends JpaRepository<ReportComment, Long> , JpaSpecificationExecutor<ReportComment> {
    List<ReportComment> findByReporterId(Long reporterId);
    boolean existsByReporterIdAndComment_Id(Long reporterId, Long commentId);
}
