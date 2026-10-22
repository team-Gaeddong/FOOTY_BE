package com.gaeddong.footy.domain.game.presentation;

import com.gaeddong.footy.domain.game.facade.GameFacade;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameRequest;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameResponse;
import com.gaeddong.footy.domain.game.presentation.dto.ListGamesResponse;
import com.gaeddong.footy.global.auth.CurrentMemberIdProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/games")
public class GameController {

    private final GameFacade gameFacade;
    private final CurrentMemberIdProvider currentMemberIdProvider;

    public GameController(GameFacade gameFacade, CurrentMemberIdProvider currentMemberIdProvider) {
        this.gameFacade = gameFacade;
        this.currentMemberIdProvider = currentMemberIdProvider;
    }

    @PostMapping
    public ResponseEntity<CreateGameResponse> create(@Valid @RequestBody CreateGameRequest request) {
        Long hostId = currentMemberIdProvider.currentMemberId();
        CreateGameResponse response = gameFacade.create(hostId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ListGamesResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return gameFacade.list(page, size);
    }
}
