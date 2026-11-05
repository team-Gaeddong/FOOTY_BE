package com.gaeddong.footy.domain.balance.service;

import com.gaeddong.footy.domain.balance.domain.BalanceReason;
import com.gaeddong.footy.domain.balance.domain.BalanceReview;
import com.gaeddong.footy.domain.balance.domain.BalanceReviewReason;
import com.gaeddong.footy.domain.balance.domain.repository.BalanceReviewReasonRepository;
import com.gaeddong.footy.domain.balance.domain.repository.BalanceReviewRepository;
import com.gaeddong.footy.domain.balance.exception.BalanceErrorCode;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.domain.member.exception.MemberErrorCode;
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

    private final MemberRepository memberRepository;
    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final BalanceReviewRepository balanceReviewRepository;
    private final BalanceReviewReasonRepository balanceReviewReasonRepository;
    private final Clock clock;

    @Transactional
    public CreatedReview create(Long writerId, Long gameId, int score, List<BalanceReason> reasons, String comment) {
        List<BalanceReason> selected = reasons == null ? List.of() : reasons;
        if (new HashSet<>(selected).size() != selected.size()) {
            throw new FootyException(BalanceErrorCode.DUPLICATE_REASON);
        }
        // 탈퇴한 회원은 없는 회원으로 본다(D-16)
        Member writer = memberRepository.findById(writerId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new FootyException(MemberErrorCode.MEMBER_NOT_FOUND));
        Game game = gameRepository.findById(gameId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new FootyException(GameErrorCode.GAME_NOT_FOUND));
        if (!gamePlayerRepository.existsByGameIdAndMemberId(gameId, writerId)) {
            throw new FootyException(BalanceErrorCode.NOT_GAME_PLAYER);
        }
        if (game.getStatus() != GameStatus.FINISHED) {
            throw new FootyException(BalanceErrorCode.GAME_NOT_FINISHED);
        }
        if (balanceReviewRepository.existsByGameIdAndWriterId(gameId, writerId)) {
            throw new FootyException(BalanceErrorCode.ALREADY_REVIEWED);
        }

        BalanceReview review = save(BalanceReview.builder()
                .game(game)
                .writer(writer)
                .score(score)
                // 공백만 입력한 의견은 남기지 않는다(D-27)
                .comment(comment == null || comment.isBlank() ? null : comment)
                .createdAt(LocalDateTime.now(clock))
                .build());
        List<BalanceReviewReason> savedReasons = balanceReviewReasonRepository.saveAll(
                selected.stream()
                        .map(reason -> BalanceReviewReason.builder()
                                .balanceReview(review)
                                .reason(reason)
                                .build())
                        .toList());
        return new CreatedReview(review, savedReasons);
    }

    // 동시에 작성하면 사전 확인을 통과해도 uk_balance_review에 걸린다
    private BalanceReview save(BalanceReview review) {
        try {
            return balanceReviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException e) {
            throw new FootyException(BalanceErrorCode.ALREADY_REVIEWED);
        }
    }

    public record CreatedReview(BalanceReview review, List<BalanceReviewReason> reasons) {
    }
}
