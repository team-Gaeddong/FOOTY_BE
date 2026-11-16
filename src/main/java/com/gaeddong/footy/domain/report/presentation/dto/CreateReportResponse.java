package com.gaeddong.footy.domain.report.presentation.dto;

import com.gaeddong.footy.domain.report.domain.Report;
import com.gaeddong.footy.domain.report.domain.ReportReason;
import com.gaeddong.footy.domain.report.domain.ReportStatus;

import java.time.LocalDateTime;

public record CreateReportResponse(
        Long id,
        Long gameId,
        Long reportedMemberId,
        ReportReason reason,
        String detail,
        ReportStatus status,
        LocalDateTime createdAt
) {

    public static CreateReportResponse from(Report report) {
        return new CreateReportResponse(
                report.getId(),
                report.getGame().getId(),
                report.getReported().getId(),
                report.getReason(),
                report.getDetail(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
