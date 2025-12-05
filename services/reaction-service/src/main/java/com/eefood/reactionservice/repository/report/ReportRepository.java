package com.eefood.reactionservice.repository.report;

import com.eefood.reactionservice.model.report.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByReporterId(Long reporterId);
    List<Report> findByStatus(com.eefood.reactionservice.enums.ReportStatus status);
    List<Report> findByTargetType(com.eefood.reactionservice.enums.ReportTargetType type);
}
