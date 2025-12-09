package com.eefood.reactionservice.repository.report;

import com.eefood.reactionservice.model.report.ReportPost;
import com.eefood.reactionservice.model.report.ReportStory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportStoryRepository extends JpaRepository<ReportStory, Long> , JpaSpecificationExecutor<ReportStory> {
    List<ReportStory> findByReporterId(Long reporterId);
    boolean existsByReporterIdAndStory_Id(Long reporterId, Long storyId);
}
