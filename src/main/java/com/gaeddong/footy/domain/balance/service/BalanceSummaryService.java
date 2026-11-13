package com.gaeddong.footy.domain.balance.service;

import com.gaeddong.footy.domain.balance.domain.BalanceReason;
import com.gaeddong.footy.domain.balance.domain.BalanceReview;
import com.gaeddong.footy.domain.balance.domain.BalanceReviewReason;
import com.gaeddong.footy.domain.balance.domain.repository.BalanceReviewReasonRepository;
import com.gaeddong.footy.domain.balance.domain.repository.BalanceReviewRepository;
import com.gaeddong.footy.domain.balance.exception.BalanceErrorCode;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.service.GameReader;
import com.gaeddong.footy.domain.lineup.domain.Lineup;
import com.gaeddong.footy.domain.lineup.domain.Team;
import com.gaeddong.footy.domain.lineup.domain.TeamMember;
import com.gaeddong.footy.domain.lineup.domain.repository.LineupRepository;
import com.gaeddong.footy.domain.lineup.domain.repository.TeamMemberRepository;
import com.gaeddong.footy.domain.lineup.domain.repository.TeamRepository;
import com.gaeddong.footy.domain.lineup.exception.LineupErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 밸런스 평가 취합(D-29). 참가 회원이 모두 작성하면 전체·팀별 평균을 계산한다.
 * 팀을 다시 짜지 않고, 파티장이 결과를 보고 직접 팀을 고칠 수 있게 보여 주기만 한다.
 */
@Service
@RequiredArgsConstructor
public class BalanceSummaryService {

    private final GameReader gameReader;
    private final GamePlayerRepository gamePlayerRepository;
    private final BalanceReviewRepository balanceReviewRepository;
    private final BalanceReviewReasonRepository balanceReviewReasonRepository;
    private final LineupRepository lineupRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional(readOnly = true)
    public Summary summarize(Long requesterId, Long gameId) {
        validateReadable(requesterId, gameId);

        long requiredCount = gamePlayerRepository.countByGameIdAndMemberDeletedFalse(gameId);
        long reviewedCount = balanceReviewRepository.countByGameId(gameId);
        if (requiredCount == 0 || reviewedCount < requiredCount) {
            return Summary.pending(requiredCount, reviewedCount);
        }

        Lineup lineup = lineupRepository.findFirstByGameIdOrderByVersionDesc(gameId)
                .orElseThrow(() -> new FootyException(LineupErrorCode.LINEUP_NOT_FOUND));
        List<ReviewDetail> reviews = loadReviews(gameId);
        List<Team> teams = teamRepository.findAllByLineupIdOrderByIdAsc(lineup.getId());
        Map<Long, Long> teamIdByMemberId = teamIdByMemberId(teams);

        Map<Long, List<ReviewDetail>> reviewsByTeamId = reviews.stream()
                .filter(review -> teamIdByMemberId.containsKey(review.writerId()))
                .collect(Collectors.groupingBy(review -> teamIdByMemberId.get(review.writerId())));
        List<TeamSummary> teamSummaries = teams.stream()
                .map(team -> TeamSummary.of(team, reviewsByTeamId.getOrDefault(team.getId(), List.of())))
                .toList();
        return new Summary(true, requiredCount, reviewedCount, average(reviews), teamSummaries);
    }

    private void validateReadable(Long requesterId, Long gameId) {
        Game game = gameReader.getGame(gameId);
        boolean readable = game.isHostedBy(requesterId)
                || gamePlayerRepository.existsByGameIdAndMemberId(gameId, requesterId);
        if (!readable) {
            throw new FootyException(BalanceErrorCode.NOT_GAME_PARTICIPANT);
        }
    }

    private List<ReviewDetail> loadReviews(Long gameId) {
        List<BalanceReview> reviews = balanceReviewRepository.findAllByGameIdOrderByIdAsc(gameId);
        Map<Long, List<BalanceReason>> reasonsByReviewId = balanceReviewReasonRepository
                .findAllByBalanceReviewIdIn(reviews.stream().map(BalanceReview::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(
                        reason -> reason.getBalanceReview().getId(),
                        Collectors.mapping(BalanceReviewReason::getReason, Collectors.toList())));
        return reviews.stream()
                .map(review -> new ReviewDetail(
                        review.getWriter().getId(),
                        review.getWriter().getNickname(),
                        review.getScore(),
                        reasonsByReviewId.getOrDefault(review.getId(), List.of()).stream().sorted().toList(),
                        review.getComment()))
                .toList();
    }

    // 비회원 선수는 작성자가 될 수 없어 회원 ID로만 팀을 찾는다
    private Map<Long, Long> teamIdByMemberId(List<Team> teams) {
        Map<Long, Long> teamIdByMemberId = new HashMap<>();
        for (TeamMember teamMember : teamMemberRepository.findAllByTeamIdInOrderByIdAsc(
                teams.stream().map(Team::getId).toList())) {
            if (teamMember.getGamePlayer().getMember() != null) {
                teamIdByMemberId.put(teamMember.getGamePlayer().getMember().getId(), teamMember.getTeam().getId());
            }
        }
        return teamIdByMemberId;
    }

    // 소수 둘째 자리에서 반올림한다. 평가가 없으면 null이다
    private static BigDecimal average(List<ReviewDetail> reviews) {
        if (reviews.isEmpty()) {
            return null;
        }
        int total = reviews.stream().mapToInt(ReviewDetail::score).sum();
        return BigDecimal.valueOf(total).divide(BigDecimal.valueOf(reviews.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * completed가 false면 아직 계산하지 않아 averageScore는 null, teams는 빈 목록이다.
     */
    public record Summary(
            boolean completed,
            long requiredCount,
            long reviewedCount,
            BigDecimal averageScore,
            List<TeamSummary> teams
    ) {

        static Summary pending(long requiredCount, long reviewedCount) {
            return new Summary(false, requiredCount, reviewedCount, null, List.of());
        }
    }

    public record TeamSummary(
            Long teamId,
            String name,
            int reviewCount,
            BigDecimal averageScore,
            Map<BalanceReason, Long> reasonCounts,
            List<ReviewDetail> reviews
    ) {

        static TeamSummary of(Team team, List<ReviewDetail> reviews) {
            // 고르지 않은 사유도 0으로 내려 화면에서 항목을 고정할 수 있게 한다
            Map<BalanceReason, Long> reasonCounts = Arrays.stream(BalanceReason.values())
                    .collect(Collectors.toMap(
                            reason -> reason,
                            reason -> reviews.stream().filter(review -> review.reasons().contains(reason)).count(),
                            (a, b) -> a,
                            () -> new EnumMap<>(BalanceReason.class)));
            return new TeamSummary(team.getId(), team.getName(), reviews.size(), average(reviews),
                    reasonCounts, reviews);
        }
    }

    public record ReviewDetail(Long writerId, String nickname, int score, List<BalanceReason> reasons,
                               String comment) {
    }
}
