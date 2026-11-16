package com.gaeddong.footy.domain.mvp.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.domain.mvp.domain.MvpVote;
import com.gaeddong.footy.domain.mvp.domain.repository.MvpVoteRepository;
import com.gaeddong.footy.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MvpVoteApiTest {

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
    private MvpVoteRepository mvpVoteRepository;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    @Test
    void 참가_회원은_종료된_경기에서_다른_참가_선수에게_MVP_투표를_한다() throws Exception {
        Member host = saveMember("host");
        Member player = saveMember("player");
        Game game = saveGame(host, GameStatus.FINISHED);
        GamePlayer hostPlayer = join(game, host);
        join(game, player);

        perform(game.getId(), player, hostPlayer.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.gameId").value(game.getId()))
                .andExpect(jsonPath("$.gamePlayerId").value(hostPlayer.getId()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        MvpVote vote = mvpVoteRepository.findAll().getFirst();
        assertThat(vote.getVoter().getId()).isEqualTo(player.getId());
        assertThat(vote.getGamePlayer().getId()).isEqualTo(hostPlayer.getId());
    }

    @Test
    void 비회원_선수에게도_투표할_수_있다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.FINISHED);
        join(game, host);
        GamePlayer guest = gamePlayerRepository.saveAndFlush(new GamePlayer(game, null, "게스트", LocalDateTime.now()));

        perform(game.getId(), host, guest.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gamePlayerId").value(guest.getId()));
    }

    @Test
    void 로그인하지_않으면_401을_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.FINISHED);

        mockMvc.perform(post("/games/{gameId}/mvp-votes", game.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gamePlayerId\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }

    @Test
    void 자기_자신에게_투표하면_400을_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.FINISHED);
        GamePlayer hostPlayer = join(game, host);

        perform(game.getId(), host, hostPlayer.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MVP_400_1"));

        assertThat(mvpVoteRepository.count()).isZero();
    }

    @Test
    void 투표_대상이_없으면_400을_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.FINISHED);
        join(game, host);

        mockMvc.perform(post("/games/{gameId}/mvp-votes", game.getId())
                        .header("Authorization", token(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
    }

    @Test
    void 경기에_참가하지_않은_회원은_403을_반환한다() throws Exception {
        Member host = saveMember("host");
        Member other = saveMember("other");
        Game game = saveGame(host, GameStatus.FINISHED);
        GamePlayer hostPlayer = join(game, host);

        perform(game.getId(), other, hostPlayer.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MVP_403_1"));

        assertThat(mvpVoteRepository.count()).isZero();
    }

    @Test
    void 다른_경기의_선수에게_투표하면_404를_반환한다() throws Exception {
        Member host = saveMember("host");
        Member player = saveMember("player");
        Game game = saveGame(host, GameStatus.FINISHED);
        Game otherGame = saveGame(host, GameStatus.FINISHED);
        join(game, host);
        GamePlayer otherGamePlayer = join(otherGame, player);

        perform(game.getId(), host, otherGamePlayer.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_2"));
        perform(game.getId(), host, Long.MAX_VALUE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_2"));
    }

    @Test
    void 존재하지_않거나_삭제된_경기는_404를_반환한다() throws Exception {
        Member host = saveMember("host");
        Member player = saveMember("player");
        Game game = saveGame(host, GameStatus.FINISHED);
        join(game, host);
        GamePlayer target = join(game, player);
        ReflectionTestUtils.setField(game, "deleted", true);
        gameRepository.flush();

        perform(game.getId(), host, target.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
        perform(Long.MAX_VALUE, host, target.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 종료되지_않은_경기는_409를_반환한다() throws Exception {
        Member host = saveMember("host");
        Member player = saveMember("player");
        Game game = saveGame(host, GameStatus.CONFIRMED);
        join(game, host);
        GamePlayer target = join(game, player);

        perform(game.getId(), host, target.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MVP_409_1"));
    }

    @Test
    void 같은_경기에_두_번_투표하면_409를_반환한다() throws Exception {
        Member host = saveMember("host");
        Member a = saveMember("a");
        Member b = saveMember("b");
        Game game = saveGame(host, GameStatus.FINISHED);
        join(game, host);
        GamePlayer aPlayer = join(game, a);
        GamePlayer bPlayer = join(game, b);

        perform(game.getId(), host, aPlayer.getId())
                .andExpect(status().isCreated());
        perform(game.getId(), host, bPlayer.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MVP_409_2"));

        assertThat(mvpVoteRepository.count()).isEqualTo(1);
    }

    private ResultActions perform(Long gameId, Member member, Long gamePlayerId) throws Exception {
        return mockMvc.perform(post("/games/{gameId}/mvp-votes", gameId)
                .header("Authorization", token(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gamePlayerId\":" + gamePlayerId + "}"));
    }

    private Member saveMember(String nickname) {
        String key = UUID.randomUUID().toString();
        Member member = Member.builder()
                .email(key + "@example.com")
                .provider(Provider.GOOGLE)
                .providerId(key)
                .build();
        member.completeOnboarding(nickname);
        return memberRepository.saveAndFlush(member);
    }

    private Game saveGame(Member host, GameStatus status) {
        Game game = Game.create(host, "주말 풋살", LocalDateTime.now().plusDays(2),
                "풋살장", 10, 2, LocalDateTime.now());
        ReflectionTestUtils.setField(game, "status", status);
        return gameRepository.saveAndFlush(game);
    }

    private GamePlayer join(Game game, Member member) {
        return gamePlayerRepository.saveAndFlush(
                new GamePlayer(game, member, member.getNickname(), LocalDateTime.now()));
    }

    private String token(Member member) {
        return "Bearer " + jwtProvider.createAccessToken(member.getId());
    }
}
