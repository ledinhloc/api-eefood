package com.eefood.reactionservice.dto.response;

import com.eefood.reactionservice.enums.ReportStatus;
import com.eefood.reactionservice.enums.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReportResponse {
    private Long id;
    private Long reporterId;
    private ReportTargetType targetType;
    private String reason;
    private ReportStatus status;
    private Long targetId;
    private LocalDateTime createdAt;
}
