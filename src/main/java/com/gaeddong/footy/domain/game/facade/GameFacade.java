package com.gaeddong.footy.domain.game.facade;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameRequest;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameResponse;
import com.gaeddong.footy.domain.game.presentation.dto.ListGamesResponse;
import com.gaeddong.footy.domain.game.service.GameService;
import org.springframework.stereotype.Component;

@Component
public class GameFacade {

    private final GameService gameService;

    public GameFacade(GameService gameService) {
        this.gameService = gameService;
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
}
