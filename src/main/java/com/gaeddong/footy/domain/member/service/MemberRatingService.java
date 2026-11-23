package com.gaeddong.footy.domain.member.service;

import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository.RatingSummary;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 회원 평점(member.rating)을 다시 계산한다(D-38).
 * 평점은 종료된 경기에서 파티장에게 받은 점수의 평균이고, 그런 경기가 2개 이상일 때부터 생긴다.
 */
@Service
@RequiredArgsConstructor
public class MemberRatingService {

    private static final int MIN_RATED_GAMES = 2;
    private static final int RATING_SCALE = 2;

    private final GamePlayerRepository gamePlayerRepository;
    private final MemberRepository memberRepository;

    /**
     * 이 경기에 참가한 회원들의 평점을 다시 계산한다. 경기를 종료한 직후에 부른다.
     */
    @Transactional
    public void refreshForGame(Long gameId) {
        refresh(gamePlayerRepository.findMemberIdsByGameId(gameId));
    }

    @Transactional
    public void refresh(Collection<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return;
        }
        Map<Long, RatingSummary> summaries = gamePlayerRepository.summarizeRatings(memberIds).stream()
                .collect(Collectors.toMap(RatingSummary::getMemberId, Function.identity()));
        for (Member member : memberRepository.findAllById(memberIds)) {
            apply(member, summaries.get(member.getId()));
        }
    }

    private void apply(Member member, RatingSummary summary) {
        if (summary == null || summary.getRatedGameCount() < MIN_RATED_GAMES) {
            member.clearRating();
            return;
        }
        member.changeRating(BigDecimal.valueOf(summary.getAverage()).setScale(RATING_SCALE, RoundingMode.HALF_UP));
    }
}
