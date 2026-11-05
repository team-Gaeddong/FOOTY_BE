package com.gaeddong.footy.domain.notice.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.notice.domain.Notice;
import com.gaeddong.footy.domain.notice.domain.repository.NoticeRepository;
import com.gaeddong.footy.domain.notice.exception.NoticeErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeQueryService {

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final NoticeRepository noticeRepository;

    @Transactional(readOnly = true)
    public List<Notice> list(Long requesterId, Long gameId) {
        Game game = findGame(gameId);
        validateParticipant(requesterId, gameId, game);
        return noticeRepository.findAllByGameIdOrderByCreatedAtDescIdDesc(gameId);
    }

    @Transactional(readOnly = true)
    public Notice get(Long requesterId, Long gameId, Long noticeId) {
        Game game = findGame(gameId);
        validateParticipant(requesterId, gameId, game);
        return noticeRepository.findByIdAndGameId(noticeId, gameId)
                .orElseThrow(() -> new FootyException(NoticeErrorCode.NOTICE_NOT_FOUND));
    }

    private Game findGame(Long gameId) {
        return gameRepository.findById(gameId)
                .filter(game -> !game.isDeleted())
                .orElseThrow(() -> new FootyException(GameErrorCode.GAME_NOT_FOUND));
    }

    private void validateParticipant(Long requesterId, Long gameId, Game game) {
        boolean host = game.getHost().getId().equals(requesterId);
        boolean participant = gamePlayerRepository.existsByGameIdAndMemberId(gameId, requesterId);
        if (!host && !participant) {
            throw new FootyException(NoticeErrorCode.NOT_GAME_PARTICIPANT);
        }
    }
}
