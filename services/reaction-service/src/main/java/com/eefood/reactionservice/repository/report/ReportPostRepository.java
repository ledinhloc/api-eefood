package com.eefood.reactionservice.repository.report;

import com.eefood.reactionservice.model.report.ReportPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportPostRepository extends JpaRepository<ReportPost, Long> {
    List<ReportPost> findByReporterId(Long reporterId);
    Page<ReportPost> findAll(Pageable pageable);
}
