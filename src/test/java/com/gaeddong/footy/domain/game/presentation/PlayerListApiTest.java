package com.gaeddong.footy.domain.game.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.domain.repository.PlayerPositionRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 경기 선수 목록 조회 API를 보안 필터·예외 처리기와 함께 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlayerListApiTest {

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

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    private Member saveMember(String email, String providerId, String nickname) {
        Member member = Member.builder()
                .email(email)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .build();
        member.completeOnboarding(nickname);
        return memberRepository.saveAndFlush(member);
    }

    private Game saveGame(Member host) {
        return gameRepository.save(Game.create(host, "일요일 풋살", LocalDateTime.now().plusDays(3), "풋살장", 12, 2,
                LocalDateTime.now()));
    }

    private GamePlayer savePlayer(Game game, Member member, LocalDateTime joinedAt, Integer skillLevel,
                                  Position... positions) {
        GamePlayer player = new GamePlayer(game, member, member.getNickname(), joinedAt);
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

    private ResultActions getPlayers(Member requester, Long gameId) throws Exception {
        return mockMvc.perform(get("/games/{gameId}/players", gameId)
                .header("Authorization", "Bearer " + jwtProvider.createAccessToken(requester.getId())));
    }

    @Test
    void 파티장이_조회하면_참여_순서와_실력까지_보인다() throws Exception {
        Member host = saveMember("host@example.com", "google-host", "파티장");
        Member other = saveMember("other@example.com", "google-other", "홍길동");
        Game game = saveGame(host);
        LocalDateTime joinedAt = LocalDateTime.of(2026, 10, 29, 12, 0);
        GamePlayer otherPlayer = savePlayer(game, other, joinedAt.plusMinutes(5), 6, Position.FW);
        GamePlayer hostPlayer = savePlayer(game, host, joinedAt, 8, Position.GK, Position.DF);

        getPlayers(host, game.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players", hasSize(2)))
                .andExpect(jsonPath("$.players[0].id").value(hostPlayer.getId()))
                .andExpect(jsonPath("$.players[0].memberId").value(host.getId()))
                .andExpect(jsonPath("$.players[0].name").value("파티장"))
                .andExpect(jsonPath("$.players[0].skillLevel").value(8))
                .andExpect(jsonPath("$.players[0].isSubstitute").value(false))
                .andExpect(jsonPath("$.players[0].positions[0]").value("GK"))
                .andExpect(jsonPath("$.players[0].positions[1]").value("DF"))
                .andExpect(jsonPath("$.players[0].joinedAt").exists())
                .andExpect(jsonPath("$.players[1].id").value(otherPlayer.getId()))
                .andExpect(jsonPath("$.players[1].skillLevel").value(6))
                .andExpect(jsonPath("$.players[1].positions[0]").value("FW"));
    }

    @Test
    void 파티장이_아닌_회원이_조회하면_실력은_null() throws Exception {
        Member host = saveMember("host@example.com", "google-host", "파티장");
        Member viewer = saveMember("viewer@example.com", "google-viewer", "구경꾼");
        Game game = saveGame(host);
        savePlayer(game, host, LocalDateTime.of(2026, 10, 29, 12, 0), 8, Position.GK);

        getPlayers(viewer, game.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players", hasSize(1)))
                .andExpect(jsonPath("$.players[0].name").value("파티장"))
                .andExpect(jsonPath("$.players[0].skillLevel").isEmpty())
                .andExpect(jsonPath("$.players[0].positions[0]").value("GK"));
    }

    @Test
    void 참가_선수가_없으면_빈_목록() throws Exception {
        Member host = saveMember("host@example.com", "google-host", "파티장");
        Game game = saveGame(host);

        getPlayers(host, game.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players", hasSize(0)));
    }

    @Test
    void 없는_경기면_404() throws Exception {
        Member member = saveMember("player@example.com", "google-player", "홍길동");

        getPlayers(member, 999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 삭제된_경기면_404() throws Exception {
        Member host = saveMember("host@example.com", "google-host", "파티장");
        Game game = saveGame(host);
        ReflectionTestUtils.setField(game, "deleted", true);
        gameRepository.saveAndFlush(game);

        getPlayers(host, game.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 토큰_없이_호출하면_401() throws Exception {
        mockMvc.perform(get("/games/1/players"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }
}
