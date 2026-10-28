package com.gaeddong.footy.domain.game.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RematchGameApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private Clock clock;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    @Test
    void 원본_파티장이_종료된_경기를_새_일시로_다시_생성한다() throws Exception {
        Member host = saveMember("host@example.com", "host-sub");
        Game source = saveGame(host, GameStatus.FINISHED, false);
        // 초가 0이면 LocalDateTime.toString()이 초를 생략해 JSON과 달라지므로 초를 고정한다
        LocalDateTime nextDate = LocalDateTime.now(clock).plusDays(1).withSecond(30).withNano(0);

        mockMvc.perform(post("/games/{gameId}/rematch", source.getId())
                        .header("Authorization", bearer(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(nextDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hostId").value(host.getId()))
                .andExpect(jsonPath("$.title").value("주말 풋살"))
                .andExpect(jsonPath("$.playDate").value(nextDate.toString()))
                .andExpect(jsonPath("$.location").value("대구 풋살장"))
                .andExpect(jsonPath("$.maxPlayers").value(12))
                .andExpect(jsonPath("$.teamCount").value(2))
                .andExpect(jsonPath("$.status").value("RECRUITING"))
                .andExpect(jsonPath("$.isDeleted").doesNotExist());

        assertThat(gameRepository.count()).isEqualTo(2);
        assertThat(gameRepository.findById(source.getId()).orElseThrow().getStatus()).isEqualTo(GameStatus.FINISHED);
        Game rematch = gameRepository.findAll().stream()
                .filter(game -> !game.getId().equals(source.getId()))
                .findFirst().orElseThrow();
        assertThat(rematch.getHost().getId()).isEqualTo(host.getId());
        assertThat(rematch.getPlayDate()).isEqualTo(nextDate);
        assertThat(rematch.isDeleted()).isFalse();
        assertThat(rematch.getCreatedAt()).isNotNull();
    }

    @Test
    void 인증하지_않으면_401이다() throws Exception {
        mockMvc.perform(post("/games/{gameId}/rematch", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(futureDate())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }

    @Test
    void 원본_파티장이_아니면_403이다() throws Exception {
        Member host = saveMember("host@example.com", "host-sub");
        Member other = saveMember("other@example.com", "other-sub");
        Game source = saveGame(host, GameStatus.FINISHED, false);

        mockMvc.perform(post("/games/{gameId}/rematch", source.getId())
                        .header("Authorization", bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(futureDate())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_403_1"));

        assertThat(gameRepository.count()).isEqualTo(1);
    }

    @Test
    void 없거나_삭제된_원본은_404이다() throws Exception {
        Member host = saveMember("host@example.com", "host-sub");
        Game deleted = saveGame(host, GameStatus.FINISHED, true);

        for (Long gameId : new Long[]{999999L, deleted.getId()}) {
            mockMvc.perform(post("/games/{gameId}/rematch", gameId)
                            .header("Authorization", bearer(host))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request(futureDate())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("GAME_404_1"));
        }
    }

    @Test
    void 종료되지_않은_원본은_409이다() throws Exception {
        Member host = saveMember("host@example.com", "host-sub");
        for (GameStatus gameStatus : new GameStatus[]{GameStatus.RECRUITING, GameStatus.CONFIRMED}) {
            Game source = saveGame(host, gameStatus, false);

            mockMvc.perform(post("/games/{gameId}/rematch", source.getId())
                            .header("Authorization", bearer(host))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request(futureDate())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("GAME_409_2"));
        }
    }

    @Test
    void 새_일시가_없거나_과거이면_400이다() throws Exception {
        Member host = saveMember("host@example.com", "host-sub");
        Game source = saveGame(host, GameStatus.FINISHED, false);

        mockMvc.perform(post("/games/{gameId}/rematch", source.getId())
                        .header("Authorization", bearer(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/games/{gameId}/rematch", source.getId())
                        .header("Authorization", bearer(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(LocalDateTime.now(clock).minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GAME_400_3"));

        assertThat(gameRepository.count()).isEqualTo(1);
    }

    private Member saveMember(String email, String providerId) {
        Member member = Member.builder()
                .email(email)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .build();
        member.completeOnboarding("호스트");
        return memberRepository.saveAndFlush(member);
    }

    private Game saveGame(Member host, GameStatus status, boolean deleted) {
        Game game = Game.create(host, "주말 풋살", LocalDateTime.of(2020, 6, 1, 10, 0),
                "대구 풋살장", 12, 2, LocalDateTime.now(clock).minusDays(1));
        ReflectionTestUtils.setField(game, "status", status);
        ReflectionTestUtils.setField(game, "deleted", deleted);
        return gameRepository.saveAndFlush(game);
    }

    private String bearer(Member member) {
        return "Bearer " + jwtProvider.createAccessToken(member.getId());
    }

    private LocalDateTime futureDate() {
        return LocalDateTime.now(clock).plusDays(1).withNano(0);
    }

    private String request(LocalDateTime playDate) {
        return "{\"playDate\":\"" + playDate + "\"}";
    }
}
