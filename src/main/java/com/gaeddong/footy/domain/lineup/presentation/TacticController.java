package com.gaeddong.footy.domain.lineup.presentation;

import com.gaeddong.footy.domain.lineup.facade.LineupFacade;
import com.gaeddong.footy.domain.lineup.presentation.dto.TacticResponse;
import com.gaeddong.footy.domain.lineup.presentation.dto.UpdateTacticRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/games/{gameId}/teams/{teamId}/tactic")
@RequiredArgsConstructor
public class TacticController {

    private final LineupFacade lineupFacade;

    @GetMapping
    public TacticResponse get(@PathVariable Long gameId, @PathVariable Long teamId) {
        return lineupFacade.getTactic(gameId, teamId);
    }

    @PutMapping
    public TacticResponse update(@AuthenticationPrincipal Long memberId,
                                 @PathVariable Long gameId,
                                 @PathVariable Long teamId,
                                 @Valid @RequestBody UpdateTacticRequest request) {
        return lineupFacade.updateTactic(memberId, gameId, teamId, request);
    }
}
