package com.gaeddong.footy.domain.game.facade;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameRequest;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameResponse;
import com.gaeddong.footy.domain.game.presentation.dto.ListGamesResponse;
import com.gaeddong.footy.domain.game.presentation.dto.UpdateSkillLevelsRequest;
import com.gaeddong.footy.domain.game.service.GamePlayerService;
import com.gaeddong.footy.domain.game.service.GameService;
import com.gaeddong.footy.global.error.FootyException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class GameFacade {

    private final GameService gameService;
    private final GamePlayerService gamePlayerService;

    public GameFacade(GameService gameService, GamePlayerService gamePlayerService) {
        this.gameService = gameService;
        this.gamePlayerService = gamePlayerService;
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

    public void updateSkillLevels(Long memberId, Long gameId, UpdateSkillLevelsRequest request) {
        Map<Long, Integer> skillLevels = new HashMap<>();
        for (UpdateSkillLevelsRequest.PlayerSkillLevel player : request.players()) {
            if (skillLevels.put(player.gamePlayerId(), player.skillLevel()) != null) {
                throw new FootyException(GameErrorCode.DUPLICATE_GAME_PLAYER);
            }
        }
        gamePlayerService.updateSkillLevels(memberId, gameId, skillLevels);
    }
}
