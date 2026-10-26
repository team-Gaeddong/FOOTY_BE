package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.domain.member.exception.MemberErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import com.gaeddong.footy.global.error.GlobalErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
        // 탈퇴한 회원은 없는 회원으로 본다(D-16)
        Member host = memberRepository.findById(hostId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new FootyException(MemberErrorCode.MEMBER_NOT_FOUND));

        Game game = Game.create(host, title, playDate, location, maxPlayers, teamCount, LocalDateTime.now());
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
