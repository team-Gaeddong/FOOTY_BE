package com.gaeddong.footy.domain.report.presentation.dto;

import com.gaeddong.footy.domain.report.domain.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotNull Long reportedMemberId,
        @NotNull ReportReason reason,
        @Size(max = 500) String detail
) {
}
