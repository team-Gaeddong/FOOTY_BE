package com.gaeddong.footy.domain.notice.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.notice.domain.Notice;
import com.gaeddong.footy.domain.notice.domain.repository.NoticeRepository;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final GameRepository gameRepository;
    private final NoticeRepository noticeRepository;
    private final Clock clock;

    @Transactional
    public Notice create(Long requesterId, Long gameId, String title, String content) {
        Game game = gameRepository.findById(gameId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new FootyException(GameErrorCode.GAME_NOT_FOUND));
        if (!game.getHost().getId().equals(requesterId)) {
            throw new FootyException(GameErrorCode.NOT_GAME_HOST);
        }

        Notice notice = Notice.builder()
                .game(game)
                .writer(game.getHost())
                .title(title)
                .content(content)
                .createdAt(LocalDateTime.now(clock))
                .build();
        return noticeRepository.save(notice);
    }
}
