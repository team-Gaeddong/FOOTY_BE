package com.gaeddong.footy.domain.game.facade;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameRequest;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameResponse;
import com.gaeddong.footy.domain.game.presentation.dto.ListGamesResponse;
import com.gaeddong.footy.domain.game.presentation.dto.RegisterPlayerRequest;
import com.gaeddong.footy.domain.game.presentation.dto.RegisterPlayerResponse;
import com.gaeddong.footy.domain.game.presentation.dto.UpdatePlayerPositionsRequest;
import com.gaeddong.footy.domain.game.service.GameService;
import com.gaeddong.footy.domain.game.service.PlayerPositionService;
import com.gaeddong.footy.domain.game.service.PlayerRegisterService;
import com.gaeddong.footy.domain.game.service.PlayerRegisterService.RegisteredPlayer;
import org.springframework.stereotype.Component;

@Component
public class GameFacade {

    private final GameService gameService;
    private final PlayerPositionService playerPositionService;
    private final PlayerRegisterService playerRegisterService;

    public GameFacade(GameService gameService, PlayerPositionService playerPositionService,
                      PlayerRegisterService playerRegisterService) {
        this.gameService = gameService;
        this.playerPositionService = playerPositionService;
        this.playerRegisterService = playerRegisterService;
    }

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

    public RegisterPlayerResponse registerPlayer(Long requesterId, Long gameId, RegisterPlayerRequest request) {
        RegisteredPlayer registered = playerRegisterService.register(
                requesterId,
                gameId,
                request.name(),
                request.positions(),
                request.skillLevel()
        );
        return RegisterPlayerResponse.of(registered.player(), registered.positions());
    }
}
