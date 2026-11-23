package com.gaeddong.footy.domain.game.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.member.domain.Member;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GameListApiTest {

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

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    @Test
    void 삭제되지_않은_전체_경기를_날짜순으로_페이지_조회한다() throws Exception {
        Member host = saveHost();
        Member otherHost = saveHost("other@example.com", "google-other");
        Game future = saveGame(otherHost, "예정 경기", LocalDateTime.of(2030, 6, 1, 10, 0), false);
        Game past = saveGame(host, "지난 경기", LocalDateTime.of(2020, 6, 1, 10, 0), false);
        saveGame(host, "삭제 경기", LocalDateTime.of(2019, 6, 1, 10, 0), true);
        String token = jwtProvider.createAccessToken(host.getId());

        mockMvc.perform(get("/games")
                        .queryParam("page", "0")
                        .queryParam("size", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games.length()").value(1))
                .andExpect(jsonPath("$.games[0].id").value(past.getId()))
                .andExpect(jsonPath("$.games[0].hostId").value(host.getId()))
                .andExpect(jsonPath("$.games[0].title").value("지난 경기"))
                .andExpect(jsonPath("$.games[0].status").value("RECRUITING"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/games")
                        .queryParam("page", "1")
                        .queryParam("size", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games[0].id").value(future.getId()))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void 상태를_주면_그_상태의_경기만_조회한다() throws Exception {
        Member host = saveHost();
        Game recruiting = saveGame(host, "모집 중", LocalDateTime.of(2030, 6, 3, 10, 0), false);
        Game confirmed = saveGame(host, "확정", LocalDateTime.of(2030, 6, 2, 10, 0), GameStatus.CONFIRMED);
        Game finished = saveGame(host, "종료", LocalDateTime.of(2030, 6, 1, 10, 0), GameStatus.FINISHED);
        String token = jwtProvider.createAccessToken(host.getId());

        // 쉼표로 여러 개
        mockMvc.perform(get("/games")
                        .queryParam("status", "RECRUITING,CONFIRMED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.games[0].id").value(confirmed.getId()))
                .andExpect(jsonPath("$.games[1].id").value(recruiting.getId()));

        // 같은 이름을 반복해도 된다
        mockMvc.perform(get("/games")
                        .queryParam("status", "FINISHED", "CONFIRMED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.games[0].id").value(finished.getId()))
                .andExpect(jsonPath("$.games[1].id").value(confirmed.getId()));

        mockMvc.perform(get("/games")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void 역할로_내가_만든_경기와_참가한_경기를_조회한다() throws Exception {
        Member me = saveHost();
        Member other = saveHost("other@example.com", "google-other");
        Game mine = saveGame(me, "내가 만든 경기", LocalDateTime.of(2030, 6, 1, 10, 0), false);
        Game joined = saveGame(other, "참가만 한 경기", LocalDateTime.of(2030, 6, 2, 10, 0), false);
        saveGame(other, "관계없는 경기", LocalDateTime.of(2030, 6, 3, 10, 0), false);
        Game deleted = saveGame(me, "삭제한 경기", LocalDateTime.of(2030, 6, 4, 10, 0), true);
        for (Game game : new Game[]{mine, joined, deleted}) {
            join(game, me);
        }
        join(joined, other);
        String token = jwtProvider.createAccessToken(me.getId());

        mockMvc.perform(get("/games")
                        .queryParam("role", "host")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.games[0].id").value(mine.getId()));

        // 파티장은 자동 참가하므로 내가 만든 경기도 참가한 경기다
        mockMvc.perform(get("/games")
                        .queryParam("role", "joined")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.games[0].id").value(mine.getId()))
                .andExpect(jsonPath("$.games[1].id").value(joined.getId()));

        // 대문자도 받고, 상태 필터와 함께 쓸 수 있다
        mockMvc.perform(get("/games")
                        .queryParam("role", "JOINED")
                        .queryParam("status", "FINISHED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void 목록의_각_경기에_후보를_포함한_참가_인원을_준다() throws Exception {
        Member host = saveHost();
        Member player = saveHost("player@example.com", "google-player");
        Game crowded = saveGame(host, "두 명", LocalDateTime.of(2030, 6, 1, 10, 0), false);
        saveGame(host, "아무도 없음", LocalDateTime.of(2030, 6, 2, 10, 0), false);
        join(crowded, host);
        join(crowded, player);

        mockMvc.perform(get("/games")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(host.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games[0].playerCount").value(2))
                .andExpect(jsonPath("$.games[0].maxPlayers").value(12))
                .andExpect(jsonPath("$.games[1].playerCount").value(0));
    }

    @Test
    void 알_수_없는_상태나_역할은_400이다() throws Exception {
        Member host = saveHost();
        String token = jwtProvider.createAccessToken(host.getId());

        mockMvc.perform(get("/games")
                        .queryParam("status", "RECRUITING,CANCELLED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));

        mockMvc.perform(get("/games")
                        .queryParam("role", "guest")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
    }

    @Test
    void 토큰_없이_목록을_조회하면_401이다() throws Exception {
        mockMvc.perform(get("/games"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }

    @Test
    void 경기가_없으면_빈_목록을_반환한다() throws Exception {
        Member host = saveHost();

        mockMvc.perform(get("/games")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(host.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void 잘못된_페이지_값은_400이다() throws Exception {
        Member host = saveHost();
        String token = jwtProvider.createAccessToken(host.getId());

        mockMvc.perform(get("/games")
                        .queryParam("page", "-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));

        mockMvc.perform(get("/games")
                        .queryParam("size", "0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));

        mockMvc.perform(get("/games")
                        .queryParam("size", "101")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
    }

    @Test
    void 숫자가_아닌_페이지_값이나_경기_ID는_400이다() throws Exception {
        Member host = saveHost();
        String token = jwtProvider.createAccessToken(host.getId());

        mockMvc.perform(get("/games")
                        .queryParam("page", "abc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));

        mockMvc.perform(get("/games/{gameId}", "abc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
    }

    private Member saveHost() {
        return saveHost("host@example.com", "google-host");
    }

    private Member saveHost(String email, String providerId) {
        Member host = Member.builder()
                .email(email)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .build();
        host.completeOnboarding("호스트-" + providerId);
        return memberRepository.saveAndFlush(host);
    }

    private Game saveGame(Member host, String title, LocalDateTime playDate, boolean deleted) {
        Game game = Game.create(host, title, playDate, "풋살장", 12, 2, LocalDateTime.now());
        if (deleted) {
            ReflectionTestUtils.setField(game, "deleted", true);
        }
        return gameRepository.saveAndFlush(game);
    }

    private Game saveGame(Member host, String title, LocalDateTime playDate, GameStatus status) {
        Game game = Game.create(host, title, playDate, "풋살장", 12, 2, LocalDateTime.now());
        ReflectionTestUtils.setField(game, "status", status);
        return gameRepository.saveAndFlush(game);
    }

    private void join(Game game, Member member) {
        gamePlayerRepository.saveAndFlush(new GamePlayer(game, member, member.getNickname(), LocalDateTime.now()));
    }
}
