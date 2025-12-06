package com.eefood.reactionservice.dto.request;

import com.eefood.reactionservice.enums.ReportStatus;
import com.eefood.reactionservice.enums.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportRequest {
    private Long reporterId;
    private ReportTargetType targetType;
    private String reason;
    private Long targetId;
    private ReportStatus status;
}
