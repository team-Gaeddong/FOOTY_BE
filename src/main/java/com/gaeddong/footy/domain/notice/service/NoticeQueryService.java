package com.gaeddong.footy.domain.notice.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.service.GameReader;
import com.gaeddong.footy.domain.notice.domain.Notice;
import com.gaeddong.footy.domain.notice.domain.repository.NoticeRepository;
import com.gaeddong.footy.domain.notice.exception.NoticeErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 공지 조회. 파티장과 경기 참가자만 볼 수 있다(D-27).
 */
@Service
@RequiredArgsConstructor
public class NoticeQueryService {

    private final GameReader gameReader;
    private final GamePlayerRepository gamePlayerRepository;
    private final NoticeRepository noticeRepository;

    /**
     * 최신 공지부터 돌려준다. 공지가 없으면 빈 목록이다.
     */
    @Transactional(readOnly = true)
    public List<Notice> list(Long requesterId, Long gameId) {
        validateReadable(requesterId, gameId);
        return noticeRepository.findAllByGameIdOrderByCreatedAtDescIdDesc(gameId);
    }

    /**
     * 다른 경기에 속한 공지도 없는 공지로 본다.
     */
    @Transactional(readOnly = true)
    public Notice get(Long requesterId, Long gameId, Long noticeId) {
        validateReadable(requesterId, gameId);
        return noticeRepository.findByIdAndGameId(noticeId, gameId)
                .orElseThrow(() -> new FootyException(NoticeErrorCode.NOTICE_NOT_FOUND));
    }

    private void validateReadable(Long requesterId, Long gameId) {
        Game game = gameReader.getGame(gameId);
        boolean readable = game.isHostedBy(requesterId)
                || gamePlayerRepository.existsByGameIdAndMemberId(gameId, requesterId);
        if (!readable) {
            throw new FootyException(NoticeErrorCode.NOT_GAME_PARTICIPANT);
        }
    }
}
