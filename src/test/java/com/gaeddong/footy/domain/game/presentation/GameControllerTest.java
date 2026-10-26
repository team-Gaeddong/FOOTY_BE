package com.gaeddong.footy.domain.game.presentation;

import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.facade.GameFacade;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameResponse;
import com.gaeddong.footy.global.error.GlobalExceptionHandler;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GameControllerTest {

    private final GameFacade gameFacade = mock(GameFacade.class);

    // 보안 필터 없이 컨트롤러만 확인한다. 인증 없음(401)은 GameCreateApiTest에서 확인한다.
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new GameController(gameFacade))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, "access-token", List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 인증된_사용자는_경기를_생성한다() throws Exception {
        given(gameFacade.create(eq(1L), any())).willReturn(new CreateGameResponse(
                10L, 1L, "일요일 풋살", LocalDateTime.of(2026, 11, 1, 10, 0),
                "대구 풋살장", 12, 2, GameStatus.RECRUITING,
                LocalDateTime.of(2026, 10, 21, 14, 30)
        ));

        mockMvc.perform(post("/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.hostId").value(1))
                .andExpect(jsonPath("$.status").value("RECRUITING"))
                .andExpect(jsonPath("$.isDeleted").doesNotExist());
    }

    @Test
    void 필수값이_누락되면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\":\"대구 풋살장\",\"maxPlayers\":12}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
    }

    @Test
    void 제목이나_장소가_최대_길이를_초과하면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "가".repeat(101)
                                + "\",\"playDate\":\"2026-11-01T10:00:00\",\"location\":\"대구 풋살장\",\"maxPlayers\":12}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"일요일 풋살\",\"playDate\":\"2026-11-01T10:00:00\",\"location\":\""
                                + "가".repeat(201) + "\",\"maxPlayers\":12}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 최대_인원이_0_이하이면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"일요일 풋살\",\"playDate\":\"2026-11-01T10:00:00\",\"location\":\"대구 풋살장\",\"maxPlayers\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
    }

    private String validRequest() {
        return "{\"title\":\"일요일 풋살\",\"playDate\":\"2026-11-01T10:00:00\",\"location\":\"대구 풋살장\",\"maxPlayers\":12}";
    }
}
