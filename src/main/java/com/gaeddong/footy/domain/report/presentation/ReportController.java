package com.gaeddong.footy.domain.report.presentation;

import com.gaeddong.footy.domain.report.facade.ReportFacade;
import com.gaeddong.footy.domain.report.presentation.dto.CreateReportRequest;
import com.gaeddong.footy.domain.report.presentation.dto.CreateReportResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/games/{gameId}/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportFacade reportFacade;

    @PostMapping
    public ResponseEntity<CreateReportResponse> create(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long gameId,
            @Valid @RequestBody CreateReportRequest request
    ) {
        CreateReportResponse response = reportFacade.create(memberId, gameId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
