package com.gaeddong.footy.domain.game.presentation.dto;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.service.GameDetailService.GameDetail;
import com.gaeddong.footy.domain.member.domain.Member;

import java.time.LocalDateTime;

public record GameDetailResponse(
        Long id,
        String title,
        LocalDateTime playDate,
        String location,
        int maxPlayers,
        int teamCount,
        GameStatus status,
        LocalDateTime createdAt,
        HostResponse host,
        long playerCount,
        boolean isHost,
        boolean isJoined
) {
    public static GameDetailResponse from(GameDetail detail) {
        Game game = detail.game();
        return new GameDetailResponse(
                game.getId(),
                game.getTitle(),
                game.getPlayDate(),
                game.getLocation(),
                game.getMaxPlayers(),
                game.getTeamCount(),
                game.getStatus(),
                game.getCreatedAt(),
                HostResponse.from(game.getHost()),
                detail.playerCount(),
                detail.host(),
                detail.joined()
        );
    }

    public record HostResponse(Long id, String nickname) {
        static HostResponse from(Member host) {
            return new HostResponse(host.getId(), host.getNickname());
        }
    }
}
