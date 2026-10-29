package com.gaeddong.footy.domain.lineup.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.domain.repository.PlayerPositionRepository;
import com.gaeddong.footy.domain.lineup.domain.repository.LineupRepository;
import com.gaeddong.footy.domain.lineup.domain.repository.TeamMemberRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 팀 짜기 API를 보안 필터·예외 처리기·실제 저장과 함께 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LineupApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private PlayerPositionRepository playerPositionRepository;

    @Autowired
    private LineupRepository lineupRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    private static final LocalDateTime JOINED_AT = LocalDateTime.of(2026, 10, 29, 12, 0);

    private Member saveMember(String key) {
        Member member = Member.builder()
                .email(key + "@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-" + key)
                .build();
        member.completeOnboarding(key);
        return memberRepository.saveAndFlush(member);
    }

    private Game saveGame(Member host) {
        return saveGame(host, 12);
    }

    private Game saveGame(Member host, int maxPlayers) {
        return gameRepository.save(Game.create(host, "일요일 풋살", LocalDateTime.now().plusDays(3), "풋살장", maxPlayers,
                2, LocalDateTime.now()));
    }

    private GamePlayer savePlayer(Game game, Member member, int order, Integer skillLevel, Position... positions) {
        GamePlayer player = new GamePlayer(game, member, member.getNickname(), JOINED_AT.plusMinutes(order));
        if (skillLevel != null) {
            player.changeSkillLevel(skillLevel);
        }
        gamePlayerRepository.save(player);
        for (int i = 0; i < positions.length; i++) {
            playerPositionRepository.save(PlayerPosition.builder()
                    .gamePlayer(player)
                    .position(positions[i])
                    .priority(i + 1)
                    .build());
        }
        return player;
    }

    private ResultActions createLineup(Member requester, Long gameId) throws Exception {
        return mockMvc.perform(post("/games/{gameId}/lineups", gameId)
                .header("Authorization", "Bearer " + jwtProvider.createAccessToken(requester.getId())));
    }

    @Test
    void 파티장이_팀을_짜면_실력_순으로_두_팀에_나눠_저장한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host);
        GamePlayer hostPlayer = savePlayer(game, host, 0, 6, Position.MF);
        GamePlayer p1 = savePlayer(game, saveMember("p1"), 1, 9, Position.FW, Position.MF);
        GamePlayer p2 = savePlayer(game, saveMember("p2"), 2, 4, Position.GK);
        GamePlayer p3 = savePlayer(game, saveMember("p3"), 3, 8);

        // 9(p1) → A, 8(p3) → B, 6(host) → A, 4(p2) → B
        createLineup(host, game.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lineupId").isNumber())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.method").value("ALGORITHM"))
                .andExpect(jsonPath("$.isConfirmed").value(false))
                .andExpect(jsonPath("$.teams", hasSize(2)))
                .andExpect(jsonPath("$.teams[0].teamId").isNumber())
                .andExpect(jsonPath("$.teams[0].name").value("A팀"))
                .andExpect(jsonPath("$.teams[0].totalSkill").value(15))
                .andExpect(jsonPath("$.teams[0].members", hasSize(2)))
                .andExpect(jsonPath("$.teams[0].members[0].gamePlayerId").value(p1.getId()))
                .andExpect(jsonPath("$.teams[0].members[0].name").value("p1"))
                .andExpect(jsonPath("$.teams[0].members[0].skillLevel").value(9))
                .andExpect(jsonPath("$.teams[0].members[0].position").value("FW"))
                .andExpect(jsonPath("$.teams[0].members[0].isSubstitute").value(false))
                .andExpect(jsonPath("$.teams[0].members[1].gamePlayerId").value(hostPlayer.getId()))
                .andExpect(jsonPath("$.teams[0].members[1].position").value("MF"))
                .andExpect(jsonPath("$.teams[1].name").value("B팀"))
                .andExpect(jsonPath("$.teams[1].totalSkill").value(12))
                .andExpect(jsonPath("$.teams[1].members[0].gamePlayerId").value(p3.getId()))
                .andExpect(jsonPath("$.teams[1].members[0].position").value("FW"))
                .andExpect(jsonPath("$.teams[1].members[1].gamePlayerId").value(p2.getId()))
                .andExpect(jsonPath("$.teams[1].members[1].position").value("GK"));

        assertThat(lineupRepository.existsByGameId(game.getId())).isTrue();
        assertThat(teamMemberRepository.count()).isEqualTo(4);
    }

    @Test
    void 최대_인원을_넘으면_실력이_낮은_선수를_후보로_저장한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, 2);
        GamePlayer hostPlayer = savePlayer(game, host, 0, 6);
        GamePlayer p1 = savePlayer(game, saveMember("p1"), 1, 9);
        GamePlayer p2 = savePlayer(game, saveMember("p2"), 2, 4);

        // 선발 9(p1) → A, 6(host) → B / 후보 4(p2) → A
        createLineup(host, game.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.teams[0].totalSkill").value(9))
                .andExpect(jsonPath("$.teams[0].members", hasSize(2)))
                .andExpect(jsonPath("$.teams[0].members[0].gamePlayerId").value(p1.getId()))
                .andExpect(jsonPath("$.teams[0].members[0].isSubstitute").value(false))
                .andExpect(jsonPath("$.teams[0].members[1].gamePlayerId").value(p2.getId()))
                .andExpect(jsonPath("$.teams[0].members[1].isSubstitute").value(true))
                .andExpect(jsonPath("$.teams[1].totalSkill").value(6))
                .andExpect(jsonPath("$.teams[1].members[0].gamePlayerId").value(hostPlayer.getId()))
                .andExpect(jsonPath("$.teams[1].members[0].isSubstitute").value(false));

        gamePlayerRepository.flush();
        assertThat(gamePlayerRepository.findById(p2.getId()).orElseThrow().isSubstitute()).isTrue();
        assertThat(gamePlayerRepository.findById(p1.getId()).orElseThrow().isSubstitute()).isFalse();
    }

    @Test
    void 이미_팀을_짠_경기면_409() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host);
        savePlayer(game, host, 0, 6);
        savePlayer(game, saveMember("p1"), 1, 7);
        createLineup(host, game.getId()).andExpect(status().isCreated());

        createLineup(host, game.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LINEUP_409_1"));
    }

    @Test
    void 실력이_없는_선수가_있으면_409() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host);
        savePlayer(game, host, 0, 6);
        savePlayer(game, saveMember("p1"), 1, null);

        createLineup(host, game.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LINEUP_409_2"));
        assertThat(lineupRepository.existsByGameId(game.getId())).isFalse();
    }

    @Test
    void 선수가_2명_미만이면_409() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host);
        savePlayer(game, host, 0, 6);

        createLineup(host, game.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LINEUP_409_3"));
    }

    @Test
    void 파티장이_아니면_403() throws Exception {
        Member host = saveMember("host");
        Member other = saveMember("other");
        Game game = saveGame(host);

        createLineup(other, game.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_403_1"));
    }

    @Test
    void 없는_경기면_404() throws Exception {
        createLineup(saveMember("host"), 999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 삭제된_경기면_404() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host);
        ReflectionTestUtils.setField(game, "deleted", true);
        gameRepository.saveAndFlush(game);

        createLineup(host, game.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 토큰_없이_호출하면_401() throws Exception {
        mockMvc.perform(post("/games/1/lineups"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }
}
