package com.gaeddong.footy.domain.lineup.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.lineup.domain.Lineup;
import com.gaeddong.footy.domain.lineup.domain.LineupMethod;
import com.gaeddong.footy.domain.lineup.domain.Team;
import com.gaeddong.footy.domain.lineup.domain.TeamMember;
import com.gaeddong.footy.domain.lineup.domain.repository.LineupRepository;
import com.gaeddong.footy.domain.lineup.domain.repository.TeamMemberRepository;
import com.gaeddong.footy.domain.lineup.domain.repository.TeamRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Position;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 전술 배치도 API를 보안 필터·예외 처리기·실제 저장과 함께 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TacticApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private LineupRepository lineupRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    private static final LocalDateTime JOINED_AT = LocalDateTime.of(2026, 10, 29, 12, 0);

    private Member host;
    private Member other;
    private Game game;
    private Lineup lineup;
    private Team teamA;
    private Team teamB;
    private GamePlayer p1;
    private GamePlayer p2;
    private GamePlayer p3;

    @BeforeEach
    void setUp() {
        host = saveMember("host");
        other = saveMember("other");
        game = gameRepository.save(Game.create(host, "일요일 풋살", LocalDateTime.now().plusDays(3), "풋살장", 12, 2,
                LocalDateTime.now()));
        p1 = savePlayer(host, 0);
        p2 = savePlayer(other, 1);
        p3 = savePlayer(saveMember("p3"), 2);
        lineup = lineupRepository.save(Lineup.builder()
                .game(game)
                .version(1)
                .method(LineupMethod.ALGORITHM)
                .createdAt(JOINED_AT)
                .build());
        teamA = teamRepository.save(Team.builder().lineup(lineup).name("A팀").build());
        teamB = teamRepository.save(Team.builder().lineup(lineup).name("B팀").build());
        teamMemberRepository.saveAll(List.of(
                TeamMember.builder().team(teamA).gamePlayer(p1).position(Position.FW).build(),
                TeamMember.builder().team(teamA).gamePlayer(p2).position(Position.GK).build(),
                TeamMember.builder().team(teamB).gamePlayer(p3).position(Position.MF).build()));
    }

    private Member saveMember(String key) {
        Member member = Member.builder()
                .email(key + "@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-" + key)
                .build();
        member.completeOnboarding(key);
        return memberRepository.saveAndFlush(member);
    }

    private GamePlayer savePlayer(Member member, int order) {
        return gamePlayerRepository.save(
                new GamePlayer(game, member, member.getNickname(), JOINED_AT.plusMinutes(order)));
    }

    private ResultActions update(Member requester, Long gameId, Long teamId, String body) throws Exception {
        return mockMvc.perform(put("/games/{gameId}/teams/{teamId}/tactic", gameId, teamId)
                .header("Authorization", "Bearer " + jwtProvider.createAccessToken(requester.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions read(Member requester, Long gameId, Long teamId) throws Exception {
        return mockMvc.perform(get("/games/{gameId}/teams/{teamId}/tactic", gameId, teamId)
                .header("Authorization", "Bearer " + jwtProvider.createAccessToken(requester.getId())));
    }

    private String placement(Long gamePlayerId, String posX, String posY) {
        return "{\"gamePlayerId\": %d, \"posX\": %s, \"posY\": %s}".formatted(gamePlayerId, posX, posY);
    }

    private String body(String formation, String... placements) {
        String formationJson = formation == null ? "null" : "\"" + formation + "\"";
        return "{\"formation\": %s, \"placements\": [%s]}".formatted(formationJson, String.join(",", placements));
    }

    @Test
    void 파티장이_전술을_저장하면_새로_조회해도_배치가_유지된다() throws Exception {
        update(host, game.getId(), teamA.getId(),
                body("4-4-2", placement(p1.getId(), "50", "20.5"), placement(p2.getId(), "50", "95.25")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(teamA.getId()))
                .andExpect(jsonPath("$.name").value("A팀"))
                .andExpect(jsonPath("$.formation").value("4-4-2"))
                .andExpect(jsonPath("$.members", hasSize(2)))
                .andExpect(jsonPath("$.members[0].gamePlayerId").value(p1.getId()))
                .andExpect(jsonPath("$.members[0].name").value("host"))
                .andExpect(jsonPath("$.members[0].position").value("FW"))
                .andExpect(jsonPath("$.members[0].isSubstitute").value(false))
                .andExpect(jsonPath("$.members[0].posX").value(50))
                .andExpect(jsonPath("$.members[0].posY").value(20.5))
                .andExpect(jsonPath("$.members[1].posY").value(95.25));

        entityManager.flush();
        entityManager.clear();

        // 다른 회원도 조회할 수 있다
        read(other, game.getId(), teamA.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formation").value("4-4-2"))
                .andExpect(jsonPath("$.members[0].posX").value(50.0))
                .andExpect(jsonPath("$.members[0].posY").value(20.5))
                .andExpect(jsonPath("$.members[1].posX").value(50.0))
                .andExpect(jsonPath("$.members[1].posY").value(95.25));
    }

    @Test
    void 다시_저장하면_빠진_선수는_배치_해제된다() throws Exception {
        update(host, game.getId(), teamA.getId(),
                body("4-4-2", placement(p1.getId(), "10", "10"), placement(p2.getId(), "20", "20")))
                .andExpect(status().isOk());

        update(host, game.getId(), teamA.getId(), body("3-5-2", placement(p2.getId(), "30", "30")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formation").value("3-5-2"))
                .andExpect(jsonPath("$.members[0].posX").isEmpty())
                .andExpect(jsonPath("$.members[0].posY").isEmpty())
                .andExpect(jsonPath("$.members[1].posX").value(30));
    }

    @Test
    void 배치_전에는_좌표가_null이다() throws Exception {
        read(host, game.getId(), teamB.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formation").isEmpty())
                .andExpect(jsonPath("$.members", hasSize(1)))
                .andExpect(jsonPath("$.members[0].posX").isEmpty());
    }

    @Test
    void 파티장이_아니면_403이다() throws Exception {
        update(other, game.getId(), teamA.getId(), body("4-4-2", placement(p1.getId(), "10", "10")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_403_1"));
    }

    @Test
    void 다른_경기의_팀이면_404이다() throws Exception {
        Game otherGame = gameRepository.save(Game.create(host, "다른 경기", LocalDateTime.now().plusDays(3), "풋살장",
                12, 2, LocalDateTime.now()));

        update(host, otherGame.getId(), teamA.getId(), body(null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LINEUP_404_1"));
        read(host, otherGame.getId(), teamA.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LINEUP_404_1"));
    }

    @Test
    void 없는_경기면_404이다() throws Exception {
        read(host, game.getId() + 999, teamA.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 확정된_팀이면_409이다() throws Exception {
        Lineup confirmed = lineupRepository.save(Lineup.builder()
                .game(game)
                .version(2)
                .method(LineupMethod.ALGORITHM)
                .confirmed(true)
                .createdAt(JOINED_AT)
                .build());
        Team team = teamRepository.save(Team.builder().lineup(confirmed).name("A팀").build());

        update(host, game.getId(), team.getId(), body("4-4-2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LINEUP_409_4"));
    }

    @Test
    void 다른_팀_선수를_배치하면_400이다() throws Exception {
        update(host, game.getId(), teamA.getId(), body("4-4-2", placement(p3.getId(), "10", "10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LINEUP_400_2"));
    }

    @Test
    void 같은_선수를_두_번_배치하면_400이다() throws Exception {
        update(host, game.getId(), teamA.getId(),
                body("4-4-2", placement(p1.getId(), "10", "10"), placement(p1.getId(), "20", "20")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LINEUP_400_1"));
    }

    @Test
    void 좌표가_범위를_벗어나거나_소수_2자리를_넘으면_400이다() throws Exception {
        for (String[] coordinate : List.of(
                new String[]{"-0.01", "10"},
                new String[]{"10", "100.01"},
                new String[]{"10.123", "10"},
                new String[]{"null", "10"})) {
            update(host, game.getId(), teamA.getId(),
                    body("4-4-2", placement(p1.getId(), coordinate[0], coordinate[1])))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
        }
    }

    @Test
    void 배치가_11명을_넘거나_포메이션이_20자를_넘으면_400이다() throws Exception {
        String[] twelve = new String[12];
        for (int i = 0; i < twelve.length; i++) {
            twelve[i] = placement(p1.getId() + 1000 + i, "10", "10");
        }
        update(host, game.getId(), teamA.getId(), body("4-4-2", twelve))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));

        update(host, game.getId(), teamA.getId(), body("1".repeat(21)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));

        update(host, game.getId(), teamA.getId(), "{\"formation\": \"4-4-2\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
    }

    @Test
    void 토큰_없이_요청하면_401이다() throws Exception {
        mockMvc.perform(get("/games/{gameId}/teams/{teamId}/tactic", game.getId(), teamA.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
        mockMvc.perform(put("/games/{gameId}/teams/{teamId}/tactic", game.getId(), teamA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("4-4-2")))
                .andExpect(status().isUnauthorized());
        assertThat(teamRepository.findById(teamA.getId()).orElseThrow().getFormation()).isNull();
    }
}
