package com.eefood.reactionservice.repository.report;

import com.eefood.reactionservice.model.report.ReportStory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportStoryRepository extends JpaRepository<ReportStory, Long> {
    List<ReportStory> findByReporterId(Long reporterId);
}
