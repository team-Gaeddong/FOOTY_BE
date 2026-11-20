package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.global.error.FootyException;
import com.gaeddong.footy.global.error.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameListService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort LIST_ORDER = Sort.by(Sort.Direction.ASC, "playDate", "id");

    private final GameRepository gameRepository;

    @Transactional(readOnly = true)
    public Page<Game> list(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new FootyException(GlobalErrorCode.INVALID_INPUT);
        }
        return gameRepository.findByDeletedFalse(PageRequest.of(page, size, LIST_ORDER));
    }
}
