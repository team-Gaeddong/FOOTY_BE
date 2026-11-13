package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RematchGameService {

    private final GameReader gameReader;
    private final GameService gameService;
    private final Clock clock;

    /**
     * 종료된 경기의 제목·장소·최대 인원·팀 수를 그대로 써서 새 경기를 만든다. 원본 파티장만 할 수 있다(D-18).
     * 원본 경기는 그대로 두고 참가자는 복사하지 않는다.
     */
    @Transactional
    public Game create(Long requesterId, Long sourceGameId, LocalDateTime playDate) {
        Game source = gameReader.getGameAsHost(sourceGameId, requesterId);
        if (!source.isFinished()) {
            throw new FootyException(GameErrorCode.GAME_NOT_FINISHED);
        }
        // 생성 정책(D-14)도 같은 검사를 하지만, 원본에서 복사한 값보다 요청 값의 오류를 먼저 알린다
        if (!playDate.isAfter(LocalDateTime.now(clock))) {
            throw new FootyException(GameErrorCode.PAST_PLAY_DATE);
        }

        return gameService.create(
                requesterId,
                source.getTitle(),
                playDate,
                source.getLocation(),
                source.getMaxPlayers(),
                source.getTeamCount()
        );
    }
}
