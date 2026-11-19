package com.gaeddong.footy.domain.poll.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.service.GameReader;
import com.gaeddong.footy.domain.poll.domain.Poll;
import com.gaeddong.footy.domain.poll.domain.PollOption;
import com.gaeddong.footy.domain.poll.domain.repository.PollOptionRepository;
import com.gaeddong.footy.domain.poll.domain.repository.PollRepository;
import com.gaeddong.footy.domain.poll.exception.PollErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PollService {

    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 10;

    private final GameReader gameReader;
    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final Clock clock;

    /**
     * 파티장이 의견 투표를 만든다. 삭제되지 않은 경기면 상태와 관계없이 만들 수 있다(D-34).
     * 선택지는 2~10개이고 서로 달라야 하며, 마감 일시는 생략하거나 현재 이후여야 한다.
     */
    @Transactional
    public Poll create(Long requesterId, Long gameId, String title, List<String> options,
                       boolean allowMultiple, LocalDateTime deadline) {
        validateOptions(options);
        Game game = gameReader.getGameAsHost(gameId, requesterId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (deadline != null && !deadline.isAfter(now)) {
            throw new FootyException(PollErrorCode.PAST_DEADLINE);
        }

        Poll poll = pollRepository.save(Poll.builder()
                .game(game)
                .creator(game.getHost())
                .title(title)
                .allowMultiple(allowMultiple)
                .deadline(deadline)
                .createdAt(now)
                .build());
        pollOptionRepository.saveAll(options.stream()
                .map(content -> PollOption.builder().poll(poll).content(content).build())
                .toList());
        return poll;
    }

    private void validateOptions(List<String> options) {
        if (options.size() < MIN_OPTIONS
                || options.size() > MAX_OPTIONS
                || new HashSet<>(options).size() != options.size()) {
            throw new FootyException(PollErrorCode.INVALID_OPTIONS);
        }
    }
}
