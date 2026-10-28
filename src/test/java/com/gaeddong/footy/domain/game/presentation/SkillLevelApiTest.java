package com.gaeddong.footy.domain.game.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.global.security.jwt.JwtProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SkillLevelApiTest {

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
    private EntityManager entityManager;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    private Member host;
    private Game game;
    private GamePlayer member;
    private GamePlayer guest;

    @BeforeEach
    void setUp() {
        host = saveMember("host@example.com", "google-host");
        game = gameRepository.saveAndFlush(
                Game.create(host, "일요일 풋살", LocalDateTime.of(2030, 6, 1, 10, 0), "풋살장", 12, 2,
                        LocalDateTime.now()));
        member = gamePlayerRepository.saveAndFlush(new GamePlayer(game, host, "호스트", LocalDateTime.now()));
        guest = gamePlayerRepository.saveAndFlush(new GamePlayer(game, null, "비회원", LocalDateTime.now()));
    }

    @Test
    void 파티장이_선수_실력을_한_번에_매긴다() throws Exception {
        String body = """
                {"players": [{"gamePlayerId": %d, "skillLevel": 1}, {"gamePlayerId": %d, "skillLevel": 10}]}
                """.formatted(member.getId(), guest.getId());

        request(host, game.getId(), body).andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();
        assertThat(gamePlayerRepository.findById(member.getId()).orElseThrow().getSkillLevel()).isEqualTo(1);
        assertThat(gamePlayerRepository.findById(guest.getId()).orElseThrow().getSkillLevel()).isEqualTo(10);
    }

    @Test
    void 파티장이_아니면_403이다() throws Exception {
        Member other = saveMember("other@example.com", "google-other");

        request(other, game.getId(), single(guest.getId(), 5))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_403_1"));
    }

    @Test
    void 없는_경기는_404다() throws Exception {
        request(host, game.getId() + 1000, single(guest.getId(), 5))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 다른_경기의_선수가_섞이면_404이고_아무것도_바꾸지_않는다() throws Exception {
        Game otherGame = gameRepository.saveAndFlush(
                Game.create(host, "다른 경기", LocalDateTime.of(2030, 7, 1, 10, 0), "풋살장", 12, 2,
                        LocalDateTime.now()));
        GamePlayer otherPlayer = gamePlayerRepository.saveAndFlush(new GamePlayer(otherGame, null, "다른 선수", LocalDateTime.now()));
        String body = """
                {"players": [{"gamePlayerId": %d, "skillLevel": 5}, {"gamePlayerId": %d, "skillLevel": 5}]}
                """.formatted(guest.getId(), otherPlayer.getId());

        request(host, game.getId(), body)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_2"));

        entityManager.flush();
        entityManager.clear();
        assertThat(gamePlayerRepository.findById(guest.getId()).orElseThrow().getSkillLevel()).isNull();
    }

    @Test
    void 같은_선수를_중복해서_보내면_400이다() throws Exception {
        String body = """
                {"players": [{"gamePlayerId": %d, "skillLevel": 5}, {"gamePlayerId": %d, "skillLevel": 6}]}
                """.formatted(guest.getId(), guest.getId());

        request(host, game.getId(), body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GAME_400_1"));
    }

    @Test
    void 점수가_1에서_10을_벗어나거나_목록이_비면_400이다() throws Exception {
        request(host, game.getId(), single(guest.getId(), 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));

        request(host, game.getId(), single(guest.getId(), 11))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));

        request(host, game.getId(), "{\"players\": []}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));

        request(host, game.getId(), "{\"players\": [{\"gamePlayerId\": %d}]}".formatted(guest.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
    }

    @Test
    void 토큰_없이_요청하면_401이다() throws Exception {
        mockMvc.perform(patch("/games/{gameId}/players/skill-levels", game.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(single(guest.getId(), 5)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }

    private ResultActions request(Member requester, Long gameId, String body) throws Exception {
        return mockMvc.perform(patch("/games/{gameId}/players/skill-levels", gameId)
                .header("Authorization", "Bearer " + jwtProvider.createAccessToken(requester.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String single(Long gamePlayerId, int skillLevel) {
        return "{\"players\": [{\"gamePlayerId\": %d, \"skillLevel\": %d}]}".formatted(gamePlayerId, skillLevel);
    }

    private Member saveMember(String email, String providerId) {
        Member saved = Member.builder()
                .email(email)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .build();
        saved.completeOnboarding("회원");
        return memberRepository.saveAndFlush(saved);
    }
}
