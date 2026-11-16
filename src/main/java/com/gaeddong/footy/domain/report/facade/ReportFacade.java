package com.gaeddong.footy.domain.report.facade;

import com.gaeddong.footy.domain.report.presentation.dto.CreateReportRequest;
import com.gaeddong.footy.domain.report.presentation.dto.CreateReportResponse;
import com.gaeddong.footy.domain.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportFacade {

    private final ReportService reportService;

    public CreateReportResponse create(Long reporterId, Long gameId, CreateReportRequest request) {
        return CreateReportResponse.from(reportService.create(
                reporterId, gameId, request.reportedMemberId(), request.reason(), request.detail()));
    }
}
