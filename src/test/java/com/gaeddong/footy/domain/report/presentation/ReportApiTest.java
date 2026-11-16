package com.gaeddong.footy.domain.report.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.domain.report.domain.Report;
import com.gaeddong.footy.domain.report.domain.ReportReason;
import com.gaeddong.footy.domain.report.domain.ReportStatus;
import com.gaeddong.footy.domain.report.domain.repository.ReportRepository;
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
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private ReportRepository reportRepository;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    @Test
    void 같은_경기에_참가한_회원을_사유와_상세_내용과_함께_신고한다() throws Exception {
        Member reporter = saveMember("reporter");
        Member reported = saveMember("reported");
        Game game = saveGame(reporter);
        join(game, reporter);
        join(game, reported);

        perform(game.getId(), reporter, new Request(reported.getId(), "ABUSE", "경기 내내 욕설을 했어요."))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.gameId").value(game.getId()))
                .andExpect(jsonPath("$.reportedMemberId").value(reported.getId()))
                .andExpect(jsonPath("$.reason").value("ABUSE"))
                .andExpect(jsonPath("$.detail").value("경기 내내 욕설을 했어요."))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        Report report = reportRepository.findAll().getFirst();
        assertThat(report.getReporter().getId()).isEqualTo(reporter.getId());
        assertThat(report.getReported().getId()).isEqualTo(reported.getId());
        assertThat(report.getGame().getId()).isEqualTo(game.getId());
        assertThat(report.getReason()).isEqualTo(ReportReason.ABUSE);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    void 상세_내용은_생략할_수_있고_공백만_있으면_저장하지_않는다() throws Exception {
        Member reporter = saveMember("reporter");
        Member first = saveMember("first");
        Member second = saveMember("second");
        Game game = saveGame(reporter);
        join(game, reporter);
        join(game, first);
        join(game, second);

        perform(game.getId(), reporter, new Request(first.getId(), "NO_SHOW", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.detail").isEmpty());
        perform(game.getId(), reporter, new Request(second.getId(), "OTHER", "   "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.detail").isEmpty());

        assertThat(reportRepository.findAll()).extracting(Report::getDetail).containsOnlyNulls();
    }

    @Test
    void 로그인하지_않으면_401을_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host);

        mockMvc.perform(post("/games/{gameId}/reports", game.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedMemberId\":1,\"reason\":\"ABUSE\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }

    @Test
    void 자기_자신을_신고하면_400을_반환한다() throws Exception {
        Member reporter = saveMember("reporter");
        Game game = saveGame(reporter);
        join(game, reporter);

        perform(game.getId(), reporter, new Request(reporter.getId(), "ABUSE", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REPORT_400_1"));

        assertThat(reportRepository.count()).isZero();
    }

    @Test
    void 경기에_참가하지_않은_회원이_신고하면_403을_반환한다() throws Exception {
        Member host = saveMember("host");
        Member outsider = saveMember("outsider");
        Game game = saveGame(host);
        join(game, host);

        perform(game.getId(), outsider, new Request(host.getId(), "ABUSE", null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REPORT_403_1"));

        assertThat(reportRepository.count()).isZero();
    }

    @Test
    void 신고_대상이_그_경기에_참가하지_않았으면_404를_반환한다() throws Exception {
        Member reporter = saveMember("reporter");
        Member outsider = saveMember("outsider");
        Game game = saveGame(reporter);
        join(game, reporter);

        perform(game.getId(), reporter, new Request(outsider.getId(), "ABUSE", null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_2"));

        assertThat(reportRepository.count()).isZero();
    }

    @Test
    void 신고_대상이_없거나_탈퇴한_회원이면_404를_반환한다() throws Exception {
        Member reporter = saveMember("reporter");
        Member withdrawn = saveMember("withdrawn");
        Game game = saveGame(reporter);
        join(game, reporter);
        join(game, withdrawn);
        withdrawn.withdraw(LocalDateTime.now());
        memberRepository.flush();

        perform(game.getId(), reporter, new Request(withdrawn.getId(), "ABUSE", null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_404_1"));
        perform(game.getId(), reporter, new Request(Long.MAX_VALUE, "ABUSE", null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_404_1"));

        assertThat(reportRepository.count()).isZero();
    }

    @Test
    void 존재하지_않거나_삭제된_경기는_404를_반환한다() throws Exception {
        Member reporter = saveMember("reporter");
        Member reported = saveMember("reported");
        Game game = saveGame(reporter);
        join(game, reporter);
        join(game, reported);
        ReflectionTestUtils.setField(game, "deleted", true);
        gameRepository.flush();

        perform(game.getId(), reporter, new Request(reported.getId(), "ABUSE", null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
        perform(Long.MAX_VALUE, reporter, new Request(reported.getId(), "ABUSE", null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 처리_대기_중인_같은_신고가_있으면_409를_반환한다() throws Exception {
        Member reporter = saveMember("reporter");
        Member reported = saveMember("reported");
        Game game = saveGame(reporter);
        join(game, reporter);
        join(game, reported);

        perform(game.getId(), reporter, new Request(reported.getId(), "ABUSE", null))
                .andExpect(status().isCreated());
        perform(game.getId(), reporter, new Request(reported.getId(), "NO_SHOW", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_409_1"));

        assertThat(reportRepository.count()).isEqualTo(1);
    }

    @Test
    void 처리가_끝난_신고나_다른_경기의_신고는_중복으로_보지_않는다() throws Exception {
        Member reporter = saveMember("reporter");
        Member reported = saveMember("reported");
        Game game = saveGame(reporter);
        Game otherGame = saveGame(reporter);
        for (Game each : new Game[]{game, otherGame}) {
            join(each, reporter);
            join(each, reported);
        }

        perform(game.getId(), reporter, new Request(reported.getId(), "ABUSE", null))
                .andExpect(status().isCreated());
        perform(otherGame.getId(), reporter, new Request(reported.getId(), "ABUSE", null))
                .andExpect(status().isCreated());
        // 상대가 나를 신고하는 것은 다른 신고다
        perform(game.getId(), reported, new Request(reporter.getId(), "ABUSE", null))
                .andExpect(status().isCreated());

        reportRepository.findAll().forEach(report ->
                ReflectionTestUtils.setField(report, "status", ReportStatus.DONE));
        reportRepository.flush();

        perform(game.getId(), reporter, new Request(reported.getId(), "VIOLENT_PLAY", null))
                .andExpect(status().isCreated());

        assertThat(reportRepository.count()).isEqualTo(4);
    }

    @Test
    void 신고_대상이나_사유가_없거나_알_수_없는_사유면_400을_반환한다() throws Exception {
        Member reporter = saveMember("reporter");
        Member reported = saveMember("reported");
        Game game = saveGame(reporter);
        join(game, reporter);
        join(game, reported);

        Request[] invalidRequests = {
                new Request(null, "ABUSE", null),
                new Request(reported.getId(), null, null),
                new Request(reported.getId(), "UNKNOWN", null),
                new Request(reported.getId(), "ABUSE", "가".repeat(501))
        };
        for (Request invalid : invalidRequests) {
            perform(game.getId(), reporter, invalid)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
        }

        assertThat(reportRepository.count()).isZero();
    }

    private ResultActions perform(Long gameId, Member member, Request request) throws Exception {
        return mockMvc.perform(post("/games/{gameId}/reports", gameId)
                .header("Authorization", token(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
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

    private Game saveGame(Member host) {
        return gameRepository.saveAndFlush(Game.create(host, "주말 풋살", LocalDateTime.now().plusDays(2),
                "풋살장", 10, 2, LocalDateTime.now()));
    }

    private void join(Game game, Member member) {
        gamePlayerRepository.saveAndFlush(new GamePlayer(game, member, member.getNickname(), LocalDateTime.now()));
    }

    private String token(Member member) {
        return "Bearer " + jwtProvider.createAccessToken(member.getId());
    }

    // 알 수 없는 사유도 보낼 수 있게 사유는 문자열로 둔다
    private record Request(Long reportedMemberId, String reason, String detail) {
    }
}
