package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.service.MemberRatingService;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 경기 후 선수 평점(game_player.rating). 경기 전 실력(skill_level)과 별개다(D-41).
 */
@Service
@RequiredArgsConstructor
public class PlayerRatingService {

    private static final BigDecimal MIN_RATING = new BigDecimal("1.0");
    private static final BigDecimal MAX_RATING = new BigDecimal("10.0");
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    private final GameReader gameReader;
    private final GamePlayerRepository gamePlayerRepository;
    private final MemberRatingService memberRatingService;

    /**
     * 파티장이 종료된 경기의 선수들에게 평점을 준다. 다시 보내면 덮어쓴다.
     * 한 명이라도 잘못되면 아무것도 반영하지 않고, 저장한 뒤 그 회원들의 평점을 바로 다시 계산한다.
     *
     * @param ratings 경기 참가자 ID(game_player_id)별 평점
     */
    @Transactional
    public void rate(Long requesterId, Long gameId, Map<Long, BigDecimal> ratings) {
        ratings.values().forEach(this::validateRating);
        Game game = gameReader.getGameAsHost(gameId, requesterId);
        if (!game.isFinished()) {
            throw new FootyException(GameErrorCode.GAME_NOT_FINISHED_FOR_RATING);
        }

        List<GamePlayer> players = gamePlayerRepository.findAllByGameIdAndIdIn(gameId, ratings.keySet());
        if (players.size() != ratings.size()) {
            throw new FootyException(GameErrorCode.GAME_PLAYER_NOT_FOUND);
        }
        if (players.stream().anyMatch(player -> isHost(game, player))) {
            throw new FootyException(GameErrorCode.HOST_SELF_RATING);
        }
        players.forEach(player -> player.changeRating(ratings.get(player.getId())));
        memberRatingService.refresh(memberIdsOf(players));
    }

    /**
     * 파티장에게 평점 대상(파티장 본인을 뺀 참가 선수)과 저장된 평점을 참여 순으로 보여 준다.
     * 선수 본인은 경기별 평점을 볼 수 없다.
     */
    @Transactional(readOnly = true)
    public List<GamePlayer> list(Long requesterId, Long gameId) {
        Game game = gameReader.getGameAsHost(gameId, requesterId);
        return gamePlayerRepository.findAllByGameIdOrderByJoinedAtAscIdAsc(gameId).stream()
                .filter(player -> !isHost(game, player))
                .toList();
    }

    // 0.5 단위인지는 2를 곱해 정수가 되는지로 본다
    private void validateRating(BigDecimal rating) {
        boolean inRange = rating.compareTo(MIN_RATING) >= 0 && rating.compareTo(MAX_RATING) <= 0;
        boolean halfStep = rating.multiply(TWO).stripTrailingZeros().scale() <= 0;
        if (!inRange || !halfStep) {
            throw new FootyException(GameErrorCode.INVALID_RATING);
        }
    }

    private boolean isHost(Game game, GamePlayer player) {
        return player.getMember() != null && game.isHostedBy(player.getMember().getId());
    }

    // 비회원 선수는 경기별 평점만 남고 회원 평점이 없다
    private List<Long> memberIdsOf(List<GamePlayer> players) {
        return players.stream()
                .map(GamePlayer::getMember)
                .filter(Objects::nonNull)
                .map(Member::getId)
                .toList();
    }
}
