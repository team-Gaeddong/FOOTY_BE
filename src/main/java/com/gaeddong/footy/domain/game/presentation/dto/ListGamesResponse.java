package com.gaeddong.footy.domain.game.presentation.dto;

import com.gaeddong.footy.domain.game.domain.Game;
import org.springframework.data.domain.Page;

import java.util.List;

public record ListGamesResponse(
        List<GameSummaryResponse> games,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static ListGamesResponse from(Page<Game> result) {
        return new ListGamesResponse(
                result.getContent().stream().map(GameSummaryResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }
}
