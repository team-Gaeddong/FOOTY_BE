package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.global.error.FootyException;
import com.gaeddong.footy.global.error.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    @Transactional
    public Game create(Long hostId, String title, LocalDateTime playDate, String location,
                       int maxPlayers, int teamCount) {
        Member host = memberRepository.findById(hostId)
                .orElseThrow(() -> new FootyException(GlobalErrorCode.UNAUTHORIZED));

        Game game = Game.create(host, title, playDate, location, maxPlayers, teamCount, LocalDateTime.now(clock));
        return gameRepository.save(game);
    }

    @Transactional(readOnly = true)
    public Page<Game> list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new FootyException(GlobalErrorCode.INVALID_INPUT);
        }

        Sort sort = Sort.by(Sort.Direction.ASC, "playDate", "id");
        return gameRepository.findByDeletedFalse(PageRequest.of(page, size, sort));
    }
}
