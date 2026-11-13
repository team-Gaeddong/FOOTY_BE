package com.gaeddong.footy.domain.lineup.facade;

import com.gaeddong.footy.domain.lineup.exception.LineupErrorCode;
import com.gaeddong.footy.domain.lineup.presentation.dto.LineupResponse;
import com.gaeddong.footy.domain.lineup.presentation.dto.TacticResponse;
import com.gaeddong.footy.domain.lineup.presentation.dto.UpdateTacticRequest;
import com.gaeddong.footy.domain.lineup.service.LineupService;
import com.gaeddong.footy.domain.lineup.service.LineupService.ViewedLineup;
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

    public LineupResponse getLatest(Long requesterId, Long gameId) {
        ViewedLineup viewed = lineupService.getLatest(requesterId, gameId);
        return LineupResponse.of(viewed.lineup(), viewed.skillVisible());
    }

    public LineupResponse confirm(Long requesterId, Long gameId) {
        return LineupResponse.from(lineupService.confirm(requesterId, gameId));
    }

    public TacticResponse getTactic(Long gameId, Long teamId) {
        return TacticResponse.from(tacticService.get(gameId, teamId));
    }

    public TacticResponse updateTactic(Long requesterId, Long gameId, Long teamId, UpdateTacticRequest request) {
        Map<Long, Coordinate> placements = toCoordinatesByPlayerId(request);
        return TacticResponse.from(tacticService.update(requesterId, gameId, teamId, request.formation(), placements));
    }

    // 같은 선수가 두 번 오면 어느 좌표를 쓸지 알 수 없으므로 거부한다
    private Map<Long, Coordinate> toCoordinatesByPlayerId(UpdateTacticRequest request) {
        Map<Long, Coordinate> coordinates = new HashMap<>();
        for (UpdateTacticRequest.Placement placement : request.placements()) {
            Coordinate coordinate = new Coordinate(placement.posX(), placement.posY());
            if (coordinates.put(placement.gamePlayerId(), coordinate) != null) {
                throw new FootyException(LineupErrorCode.DUPLICATE_PLACEMENT);
            }
        }
        return coordinates;
    }
}
