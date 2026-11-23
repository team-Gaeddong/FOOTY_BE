package com.gaeddong.footy.domain.game.presentation;

import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.facade.GameFacade;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameRequest;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameResponse;
import com.gaeddong.footy.domain.game.presentation.dto.GameDetailResponse;
import com.gaeddong.footy.domain.game.presentation.dto.JoinGameRequest;
import com.gaeddong.footy.domain.game.presentation.dto.JoinGameResponse;
import com.gaeddong.footy.domain.game.presentation.dto.ListGamePlayersResponse;
import com.gaeddong.footy.domain.game.presentation.dto.ListGamesResponse;
import com.gaeddong.footy.domain.game.presentation.dto.UpdatePlayerPositionsRequest;
import com.gaeddong.footy.domain.game.presentation.dto.UpdateSkillLevelsRequest;
import com.gaeddong.footy.domain.game.service.GameListCondition;
import com.gaeddong.footy.domain.game.service.GameListRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
public class GameController {

    private final GameFacade gameFacade;

    @PostMapping
    public ResponseEntity<CreateGameResponse> create(@AuthenticationPrincipal Long memberId,
                                                     @Valid @RequestBody CreateGameRequest request) {
        CreateGameResponse response = gameFacade.create(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ListGamesResponse list(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) List<GameStatus> status,
            @RequestParam(required = false) GameListRole role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return gameFacade.list(memberId, new GameListCondition(status, role, from, to), page, size);
    }

    @GetMapping("/{gameId}")
    public GameDetailResponse get(@AuthenticationPrincipal Long memberId, @PathVariable Long gameId) {
        return gameFacade.get(memberId, gameId);
    }

    @PatchMapping("/{gameId}/finish")
    public GameDetailResponse finish(@AuthenticationPrincipal Long memberId, @PathVariable Long gameId) {
        return gameFacade.finish(memberId, gameId);
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Long memberId, @PathVariable Long gameId) {
        gameFacade.delete(memberId, gameId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{gameId}/players")
    public ListGamePlayersResponse listPlayers(@AuthenticationPrincipal Long memberId, @PathVariable Long gameId) {
        return gameFacade.listPlayers(memberId, gameId);
    }

    @PatchMapping("/{gameId}/players/skill-levels")
    public ResponseEntity<Void> updateSkillLevels(@AuthenticationPrincipal Long memberId,
                                                  @PathVariable Long gameId,
                                                  @Valid @RequestBody UpdateSkillLevelsRequest request) {
        gameFacade.updateSkillLevels(memberId, gameId, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{gameId}/players/{gamePlayerId}/positions")
    public ResponseEntity<Void> updatePlayerPositions(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long gameId,
            @PathVariable Long gamePlayerId,
            @RequestBody UpdatePlayerPositionsRequest request
    ) {
        gameFacade.updatePlayerPositions(memberId, gameId, gamePlayerId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{gameId}/players")
    public ResponseEntity<JoinGameResponse> join(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long gameId,
            @RequestBody JoinGameRequest request
    ) {
        JoinGameResponse response = gameFacade.join(memberId, gameId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
