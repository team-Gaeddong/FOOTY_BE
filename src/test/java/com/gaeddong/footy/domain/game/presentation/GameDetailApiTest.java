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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 경기 상세 조회 API를 보안 필터·예외 처리기와 함께 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GameDetailApiTest {

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

    private void savePlayer(Game game, Member member) {
        gamePlayerRepository.save(new GamePlayer(game, member, member.getNickname(), LocalDateTime.now()));
    }

    private ResultActions getGame(Member requester, Long gameId) throws Exception {
        return mockMvc.perform(get("/games/{gameId}", gameId)
                .header("Authorization", "Bearer " + jwtProvider.createAccessToken(requester.getId())));
    }

    @Test
    void 파티장이_조회하면_경기_정보와_요약이_보인다() throws Exception {
        Member host = saveMember("host@example.com", "google-host", "파티장");
        Member other = saveMember("other@example.com", "google-other", "홍길동");
        Game game = saveGame(host);
        savePlayer(game, host);
        savePlayer(game, other);

        getGame(host, game.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(game.getId()))
                .andExpect(jsonPath("$.title").value("일요일 풋살"))
                .andExpect(jsonPath("$.playDate").exists())
                .andExpect(jsonPath("$.location").value("풋살장"))
                .andExpect(jsonPath("$.maxPlayers").value(12))
                .andExpect(jsonPath("$.teamCount").value(2))
                .andExpect(jsonPath("$.status").value("RECRUITING"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.host.id").value(host.getId()))
                .andExpect(jsonPath("$.host.nickname").value("파티장"))
                .andExpect(jsonPath("$.playerCount").value(2))
                .andExpect(jsonPath("$.isHost").value(true))
                .andExpect(jsonPath("$.isJoined").value(true));
    }

    @Test
    void 참가한_회원이_조회하면_파티장은_아니고_참가자다() throws Exception {
        Member host = saveMember("host@example.com", "google-host", "파티장");
        Member player = saveMember("player@example.com", "google-player", "홍길동");
        Game game = saveGame(host);
        savePlayer(game, host);
        savePlayer(game, player);

        getGame(player, game.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isHost").value(false))
                .andExpect(jsonPath("$.isJoined").value(true));
    }

    @Test
    void 참가하지_않은_회원도_조회할_수_있다() throws Exception {
        Member host = saveMember("host@example.com", "google-host", "파티장");
        Member viewer = saveMember("viewer@example.com", "google-viewer", "구경꾼");
        Game game = saveGame(host);
        savePlayer(game, host);

        getGame(viewer, game.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerCount").value(1))
                .andExpect(jsonPath("$.isHost").value(false))
                .andExpect(jsonPath("$.isJoined").value(false));
    }

    @Test
    void 없는_경기면_404() throws Exception {
        Member member = saveMember("player@example.com", "google-player", "홍길동");

        getGame(member, 999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 삭제된_경기면_404() throws Exception {
        Member host = saveMember("host@example.com", "google-host", "파티장");
        Game game = saveGame(host);
        ReflectionTestUtils.setField(game, "deleted", true);
        gameRepository.saveAndFlush(game);

        getGame(host, game.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 토큰_없이_호출하면_401() throws Exception {
        mockMvc.perform(get("/games/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }
}
