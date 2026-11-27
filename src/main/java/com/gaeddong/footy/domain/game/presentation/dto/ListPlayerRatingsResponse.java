package com.gaeddong.footy.domain.game.presentation.dto;

import com.gaeddong.footy.domain.game.domain.GamePlayer;

import java.math.BigDecimal;
import java.util.List;

public record ListPlayerRatingsResponse(
        Long gameId,
        List<PlayerRatingResponse> players
) {

    public static ListPlayerRatingsResponse of(Long gameId, List<GamePlayer> players) {
        return new ListPlayerRatingsResponse(gameId, players.stream().map(PlayerRatingResponse::from).toList());
    }

    /**
     * rating은 아직 주지 않았으면 null이다.
     */
    public record PlayerRatingResponse(Long gamePlayerId, Long memberId, String name, BigDecimal rating) {

        static PlayerRatingResponse from(GamePlayer player) {
            return new PlayerRatingResponse(
                    player.getId(),
                    player.getMember() == null ? null : player.getMember().getId(),
                    player.getName(),
                    player.getRating()
            );
        }
    }
}
