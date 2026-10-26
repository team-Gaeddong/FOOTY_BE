package com.gaeddong.footy.domain.game.domain.repository;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class PlayerPositionRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 26, 12, 0);

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private PlayerPositionRepository playerPositionRepository;

    private Game saveGame() {
        Member host = memberRepository.save(Member.builder()
                .email("host@gmail.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub-host")
                .build());
        return gameRepository.save(Game.create(host, "주말 풋살", NOW.plusDays(3), "잠실", 10, 2, NOW));
    }

    private void savePosition(GamePlayer player, Position position, int priority) {
        playerPositionRepository.save(PlayerPosition.builder()
                .gamePlayer(player)
                .position(position)
                .priority(priority)
                .build());
    }

    @Test
    void 선수의_포지션을_우선순위_순서로_조회한다() {
        GamePlayer player = gamePlayerRepository.save(new GamePlayer(saveGame(), null, "비회원", NOW));
        savePosition(player, Position.MF, 2);
        savePosition(player, Position.FW, 1);

        assertThat(playerPositionRepository.findAllByGamePlayerIdOrderByPriorityAsc(player.getId()))
                .extracting(PlayerPosition::getPosition, PlayerPosition::getPriority)
                .containsExactly(
                        tuple(Position.FW, 1),
                        tuple(Position.MF, 2));
    }

    @Test
    void 선수의_포지션만_지운다() {
        Game game = saveGame();
        GamePlayer player = gamePlayerRepository.save(new GamePlayer(game, null, "선수1", NOW));
        GamePlayer other = gamePlayerRepository.save(new GamePlayer(game, null, "선수2", NOW));
        savePosition(player, Position.FW, 1);
        savePosition(other, Position.GK, 1);

        playerPositionRepository.deleteAllByGamePlayerId(player.getId());

        assertThat(playerPositionRepository.findAllByGamePlayerIdOrderByPriorityAsc(player.getId())).isEmpty();
        assertThat(playerPositionRepository.findAllByGamePlayerIdOrderByPriorityAsc(other.getId())).hasSize(1);
    }

    @Test
    void 경기_참가자는_그_경기_안에서만_찾는다() {
        Game game = saveGame();
        GamePlayer player = gamePlayerRepository.save(new GamePlayer(game, null, "선수1", NOW));

        assertThat(gamePlayerRepository.findByIdAndGameId(player.getId(), game.getId())).isPresent();
        assertThat(gamePlayerRepository.findByIdAndGameId(player.getId(), game.getId() + 1)).isEmpty();
    }
}
