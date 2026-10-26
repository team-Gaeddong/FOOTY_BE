package com.gaeddong.footy.domain.game.facade;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameRequest;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameResponse;
import com.gaeddong.footy.domain.game.presentation.dto.ListGamesResponse;
import com.gaeddong.footy.domain.game.presentation.dto.UpdatePlayerPositionsRequest;
import com.gaeddong.footy.domain.game.service.GameService;
import com.gaeddong.footy.domain.game.service.PlayerPositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameFacade {

    private final GameService gameService;
    private final PlayerPositionService playerPositionService;

    public CreateGameResponse create(Long hostId, CreateGameRequest request) {
        Game game = gameService.create(
                hostId,
                request.title(),
                request.playDate(),
                request.location(),
                request.maxPlayers(),
                request.teamCount()
        );
        return CreateGameResponse.from(game);
    }

    public ListGamesResponse list(int page, int size) {
        return ListGamesResponse.from(gameService.list(page, size));
    }

    public void updatePlayerPositions(Long requesterId, Long gameId, Long gamePlayerId,
                                      UpdatePlayerPositionsRequest request) {
        playerPositionService.updatePositions(requesterId, gameId, gamePlayerId, request.positions());
    }
}
