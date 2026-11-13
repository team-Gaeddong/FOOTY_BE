package com.gaeddong.footy.domain.balance.presentation.dto;

import com.gaeddong.footy.domain.balance.domain.BalanceReason;
import com.gaeddong.footy.domain.balance.service.BalanceSummaryService.ReviewDetail;
import com.gaeddong.footy.domain.balance.service.BalanceSummaryService.Summary;
import com.gaeddong.footy.domain.balance.service.BalanceSummaryService.TeamSummary;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record BalanceSummaryResponse(
        Long gameId,
        boolean completed,
        long requiredCount,
        long reviewedCount,
        BigDecimal averageScore,
        List<TeamResponse> teams
) {

    public static BalanceSummaryResponse of(Long gameId, Summary summary) {
        return new BalanceSummaryResponse(
                gameId,
                summary.completed(),
                summary.requiredCount(),
                summary.reviewedCount(),
                summary.averageScore(),
                summary.teams().stream().map(TeamResponse::from).toList()
        );
    }

    public record TeamResponse(
            Long teamId,
            String name,
            int reviewCount,
            BigDecimal averageScore,
            Map<BalanceReason, Long> reasonCounts,
            List<ReviewResponse> reviews
    ) {

        static TeamResponse from(TeamSummary team) {
            return new TeamResponse(
                    team.teamId(),
                    team.name(),
                    team.reviewCount(),
                    team.averageScore(),
                    team.reasonCounts(),
                    team.reviews().stream().map(ReviewResponse::from).toList()
            );
        }
    }

    public record ReviewResponse(
            Long writerId,
            String nickname,
            int score,
            List<BalanceReason> reasons,
            String comment
    ) {

        static ReviewResponse from(ReviewDetail review) {
            return new ReviewResponse(
                    review.writerId(), review.nickname(), review.score(), review.reasons(), review.comment());
        }
    }
}
