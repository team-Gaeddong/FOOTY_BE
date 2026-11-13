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
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.service.MemberReader;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BalanceReviewService {

    private final MemberReader memberReader;
    private final GameReader gameReader;
    private final GamePlayerRepository gamePlayerRepository;
    private final BalanceReviewRepository balanceReviewRepository;
    private final BalanceReviewReasonRepository balanceReviewReasonRepository;
    private final Clock clock;

    /**
     * 경기에 참가한 회원이 종료된 경기의 밸런스를 한 번 평가한다(D-28).
     */
    @Transactional
    public CreatedReview create(Long writerId, Long gameId, int score, List<BalanceReason> reasons, String comment) {
        List<BalanceReason> selectedReasons = withoutDuplicates(reasons);
        Member writer = memberReader.getActiveMember(writerId);
        Game game = gameReader.getGame(gameId);
        validateReviewable(game, writerId);

        BalanceReview review = saveNewReview(BalanceReview.builder()
                .game(game)
                .writer(writer)
                .score(score)
                .comment(blankToNull(comment))
                .createdAt(LocalDateTime.now(clock))
                .build());
        List<BalanceReviewReason> savedReasons = balanceReviewReasonRepository.saveAll(
                selectedReasons.stream()
                        .map(reason -> BalanceReviewReason.builder()
                                .balanceReview(review)
                                .reason(reason)
                                .build())
                        .toList());
        return new CreatedReview(review, savedReasons);
    }

    // 사유는 선택 항목이라 생략하면 빈 목록으로 본다
    private List<BalanceReason> withoutDuplicates(List<BalanceReason> reasons) {
        List<BalanceReason> selected = reasons == null ? List.of() : reasons;
        if (new HashSet<>(selected).size() != selected.size()) {
            throw new FootyException(BalanceErrorCode.DUPLICATE_REASON);
        }
        return selected;
    }

    private void validateReviewable(Game game, Long writerId) {
        if (!gamePlayerRepository.existsByGameIdAndMemberId(game.getId(), writerId)) {
            throw new FootyException(BalanceErrorCode.NOT_GAME_PLAYER);
        }
        if (!game.isFinished()) {
            throw new FootyException(BalanceErrorCode.GAME_NOT_FINISHED);
        }
        if (balanceReviewRepository.existsByGameIdAndWriterId(game.getId(), writerId)) {
            throw new FootyException(BalanceErrorCode.ALREADY_REVIEWED);
        }
    }

    // 공백만 입력한 의견은 남기지 않는다(D-28)
    private String blankToNull(String comment) {
        return comment == null || comment.isBlank() ? null : comment;
    }

    // 동시에 작성하면 사전 확인을 통과해도 uk_balance_review에 걸린다
    private BalanceReview saveNewReview(BalanceReview review) {
        try {
            return balanceReviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException e) {
            throw new FootyException(BalanceErrorCode.ALREADY_REVIEWED);
        }
    }

    public record CreatedReview(BalanceReview review, List<BalanceReviewReason> reasons) {
    }
}
