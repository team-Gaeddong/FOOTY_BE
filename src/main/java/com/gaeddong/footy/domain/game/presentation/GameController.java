package com.gaeddong.footy.domain.game.presentation;

import com.gaeddong.footy.domain.game.facade.GameFacade;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameRequest;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameResponse;
import com.gaeddong.footy.domain.game.presentation.dto.ListGamesResponse;
import com.gaeddong.footy.domain.game.presentation.dto.UpdateSkillLevelsRequest;
import com.gaeddong.footy.domain.game.presentation.dto.UpdatePlayerPositionsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return gameFacade.list(page, size);
    }

    @PatchMapping("/{gameId}/players/skill-levels")
    public ResponseEntity<Void> updateSkillLevels(@PathVariable Long gameId,
                                                  @Valid @RequestBody UpdateSkillLevelsRequest request) {
        gameFacade.updateSkillLevels(currentMemberIdProvider.currentMemberId(), gameId, request);
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
}
