package com.gaeddong.footy.domain.lineup.facade;

import com.gaeddong.footy.domain.lineup.exception.LineupErrorCode;
import com.gaeddong.footy.domain.lineup.presentation.dto.LineupResponse;
import com.gaeddong.footy.domain.lineup.presentation.dto.TacticResponse;
import com.gaeddong.footy.domain.lineup.presentation.dto.UpdateTacticRequest;
import com.gaeddong.footy.domain.lineup.service.LineupService;
import com.gaeddong.footy.domain.lineup.service.TacticService;
import com.gaeddong.footy.domain.lineup.service.TacticService.Coordinate;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LineupFacade {

    private final LineupService lineupService;
    private final TacticService tacticService;

    public LineupResponse create(Long requesterId, Long gameId) {
        return LineupResponse.from(lineupService.create(requesterId, gameId));
    }

    public TacticResponse getTactic(Long gameId, Long teamId) {
        return TacticResponse.from(tacticService.get(gameId, teamId));
    }

    public TacticResponse updateTactic(Long requesterId, Long gameId, Long teamId, UpdateTacticRequest request) {
        Map<Long, Coordinate> placements = new HashMap<>();
        for (UpdateTacticRequest.Placement placement : request.placements()) {
            if (placements.put(placement.gamePlayerId(), new Coordinate(placement.posX(), placement.posY())) != null) {
                throw new FootyException(LineupErrorCode.DUPLICATE_PLACEMENT);
            }
        }
        return TacticResponse.from(tacticService.update(requesterId, gameId, teamId, request.formation(), placements));
    }
}
