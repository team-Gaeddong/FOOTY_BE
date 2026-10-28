package com.gaeddong.footy.domain.game.presentation.dto;

import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.service.PlayerListService.ListedPlayer;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Position;

import java.time.LocalDateTime;
import java.util.List;

public record ListGamePlayersResponse(List<GamePlayerResponse> players) {

    public static ListGamePlayersResponse from(List<ListedPlayer> players) {
        return new ListGamePlayersResponse(players.stream().map(GamePlayerResponse::from).toList());
    }

    /**
     * skillLevel은 파티장이 조회할 때만 값이 있다.
     */
    public record GamePlayerResponse(
            Long id,
            Long memberId,
            String name,
            Integer skillLevel,
            boolean isSubstitute,
            List<Position> positions,
            LocalDateTime joinedAt
    ) {
        static GamePlayerResponse from(ListedPlayer listed) {
            GamePlayer player = listed.player();
            Member member = player.getMember();
            return new GamePlayerResponse(
                    player.getId(),
                    member == null ? null : member.getId(),
                    player.getName(),
                    listed.skillLevel(),
                    player.isSubstitute(),
                    listed.positions(),
                    player.getJoinedAt()
            );
        }
    }
}
