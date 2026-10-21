package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.global.error.FootyException;
import com.gaeddong.footy.global.error.GlobalErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final MemberRepository memberRepository;

    public GameService(GameRepository gameRepository, MemberRepository memberRepository) {
        this.gameRepository = gameRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public Game create(Long hostId, String title, LocalDateTime playDate, String location,
                       int maxPlayers, int teamCount) {
        Member host = memberRepository.findById(hostId)
                .orElseThrow(() -> new FootyException(GlobalErrorCode.UNAUTHORIZED));

        Game game = Game.create(host, title, playDate, location, maxPlayers, teamCount, LocalDateTime.now());
        return gameRepository.save(game);
    }
}
