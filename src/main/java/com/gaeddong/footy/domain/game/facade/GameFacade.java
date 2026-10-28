package com.gaeddong.footy.domain.game.facade;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameRequest;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameResponse;
import com.gaeddong.footy.domain.game.presentation.dto.ListGamesResponse;
import com.gaeddong.footy.domain.game.presentation.dto.RegisterPlayerRequest;
import com.gaeddong.footy.domain.game.presentation.dto.RegisterPlayerResponse;
import com.gaeddong.footy.domain.game.presentation.dto.RematchGameRequest;
import com.gaeddong.footy.domain.game.presentation.dto.UpdatePlayerPositionsRequest;
import com.gaeddong.footy.domain.game.presentation.dto.UpdateSkillLevelsRequest;
import com.gaeddong.footy.domain.game.service.GamePlayerService;
import com.gaeddong.footy.domain.game.service.GameService;
import com.gaeddong.footy.domain.game.service.PlayerPositionService;
import com.gaeddong.footy.domain.game.service.PlayerRegisterService;
import com.gaeddong.footy.domain.game.service.PlayerRegisterService.RegisteredPlayer;
import com.gaeddong.footy.domain.game.service.RematchGameService;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GameFacade {

    private final GameService gameService;
    private final GamePlayerService gamePlayerService;
    private final PlayerPositionService playerPositionService;
    private final RematchGameService rematchGameService;
    private final PlayerRegisterService playerRegisterService;

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

    public CreateGameResponse rematch(Long requesterId, Long sourceGameId, RematchGameRequest request) {
        return CreateGameResponse.from(rematchGameService.create(requesterId, sourceGameId, request.playDate()));
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
