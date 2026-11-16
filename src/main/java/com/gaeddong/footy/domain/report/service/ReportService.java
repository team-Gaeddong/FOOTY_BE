package com.gaeddong.footy.domain.report.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.game.service.GameReader;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.service.MemberReader;
import com.gaeddong.footy.domain.report.domain.Report;
import com.gaeddong.footy.domain.report.domain.ReportReason;
import com.gaeddong.footy.domain.report.domain.ReportStatus;
import com.gaeddong.footy.domain.report.domain.repository.ReportRepository;
import com.gaeddong.footy.domain.report.exception.ReportErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final MemberReader memberReader;
    private final GameReader gameReader;
    private final GamePlayerRepository gamePlayerRepository;
    private final ReportRepository reportRepository;
    private final Clock clock;

    /**
     * 같은 경기에 참가한 회원을 신고한다(D-30).
     */
    @Transactional
    public Report create(Long reporterId, Long gameId, Long reportedId, ReportReason reason, String detail) {
        if (reporterId.equals(reportedId)) {
            throw new FootyException(ReportErrorCode.SELF_REPORT);
        }
        Member reporter = memberReader.getActiveMember(reporterId);
        Game game = gameReader.getGame(gameId);
        Member reported = memberReader.getActiveMember(reportedId);
        validateReportable(gameId, reporterId, reportedId);

        return reportRepository.save(
                Report.submit(reporter, reported, game, reason, blankToNull(detail), LocalDateTime.now(clock)));
    }

    private void validateReportable(Long gameId, Long reporterId, Long reportedId) {
        if (!gamePlayerRepository.existsByGameIdAndMemberId(gameId, reporterId)) {
            throw new FootyException(ReportErrorCode.NOT_GAME_PLAYER);
        }
        if (!gamePlayerRepository.existsByGameIdAndMemberId(gameId, reportedId)) {
            throw new FootyException(GameErrorCode.GAME_PLAYER_NOT_FOUND);
        }
        // report에는 UNIQUE 제약이 없어 동시에 보낸 요청까지는 막지 못한다(D-30)
        if (reportRepository.existsByReporterIdAndReportedIdAndGameIdAndStatus(
                reporterId, reportedId, gameId, ReportStatus.PENDING)) {
            throw new FootyException(ReportErrorCode.ALREADY_REPORTED);
        }
    }

    // 공백만 입력한 상세 내용은 남기지 않는다(D-30)
    private String blankToNull(String detail) {
        return detail == null || detail.isBlank() ? null : detail;
    }
}
