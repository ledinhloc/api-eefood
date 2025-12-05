package com.eefood.reactionservice.model;

import com.eefood.reactionservice.enums.ReportStatus;
import com.eefood.reactionservice.enums.ReportTargetType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Report extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Người báo cáo
    @Column(nullable = false)
    private Long reporterId;

    // Loại đối tượng bị report: POST, STORY, COMMENT
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportTargetType targetType;

    // Lý do báo cáo
    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportStatus status = ReportStatus.PENDING;

    // ID của đối tượng bị report
    @Column(nullable = false)
    private Long targetId;
}
