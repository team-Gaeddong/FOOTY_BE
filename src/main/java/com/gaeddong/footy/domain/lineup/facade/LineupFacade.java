package com.gaeddong.footy.domain.lineup.facade;

import com.gaeddong.footy.domain.lineup.presentation.dto.LineupResponse;
import com.gaeddong.footy.domain.lineup.service.LineupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LineupFacade {

    private final LineupService lineupService;

    public LineupResponse create(Long requesterId, Long gameId) {
        return LineupResponse.from(lineupService.create(requesterId, gameId));
    }
}
