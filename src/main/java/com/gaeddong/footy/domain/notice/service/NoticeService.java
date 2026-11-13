package com.gaeddong.footy.domain.notice.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.service.GameReader;
import com.gaeddong.footy.domain.notice.domain.Notice;
import com.gaeddong.footy.domain.notice.domain.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final GameReader gameReader;
    private final NoticeRepository noticeRepository;
    private final Clock clock;

    /**
     * 파티장이 경기 공지를 쓴다. 삭제되지 않은 경기면 상태와 관계없이 쓸 수 있다(D-26).
     */
    @Transactional
    public Notice create(Long requesterId, Long gameId, String title, String content) {
        Game game = gameReader.getGameAsHost(gameId, requesterId);

        return noticeRepository.save(Notice.builder()
                .game(game)
                .writer(game.getHost())
                .title(title)
                .content(content)
                .createdAt(LocalDateTime.now(clock))
                .build());
    }
}
