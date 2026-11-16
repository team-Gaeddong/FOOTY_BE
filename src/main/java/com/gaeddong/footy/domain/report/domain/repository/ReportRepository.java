package com.gaeddong.footy.domain.report.domain.repository;

import com.gaeddong.footy.domain.report.domain.Report;
import com.gaeddong.footy.domain.report.domain.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndReportedIdAndGameIdAndStatus(
            Long reporterId, Long reportedId, Long gameId, ReportStatus status);
}
