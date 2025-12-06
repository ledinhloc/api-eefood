package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.request.ReportRequest;
import com.eefood.reactionservice.dto.response.ReportResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @PostMapping
    public ResponseData<ReportResponse> createReport(@RequestBody ReportRequest request) {
        return new ResponseData<>(200, "Report created", reportService.createReport(request));
    }

    @GetMapping
    public ResponseData<List<ReportResponse>> getUserReports(@RequestParam Long userId) {
        return new ResponseData<>(200, "Success", reportService.getUserReports(userId));
    }

    @GetMapping("/{id}")
    public ResponseData<ReportResponse> getReportDetail(@PathVariable Long id) {
        return new ResponseData<>(200, "Success", reportService.getReportDetail(id));
    }
}
