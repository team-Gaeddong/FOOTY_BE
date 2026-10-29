package com.gaeddong.footy.domain.lineup.presentation;

import com.gaeddong.footy.domain.lineup.facade.LineupFacade;
import com.gaeddong.footy.domain.lineup.presentation.dto.LineupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/games/{gameId}/lineups")
@RequiredArgsConstructor
public class LineupController {

    private final LineupFacade lineupFacade;

    @PostMapping
    public ResponseEntity<LineupResponse> create(@AuthenticationPrincipal Long memberId, @PathVariable Long gameId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lineupFacade.create(memberId, gameId));
    }
}
