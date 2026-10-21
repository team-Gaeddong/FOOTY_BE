package com.gaeddong.footy.domain.game.presentation;

import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.facade.GameFacade;
import com.gaeddong.footy.domain.game.presentation.dto.CreateGameResponse;
import com.gaeddong.footy.global.auth.CurrentMemberIdProvider;
import com.gaeddong.footy.global.error.FootyException;
import com.gaeddong.footy.global.error.GlobalErrorCode;
import com.gaeddong.footy.global.error.GlobalExceptionHandler;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GameControllerTest {

    private final GameFacade gameFacade = mock(GameFacade.class);

    private final CurrentMemberIdProvider currentMemberIdProvider = mock(CurrentMemberIdProvider.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new GameController(gameFacade, currentMemberIdProvider))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void 인증된_사용자는_경기를_생성한다() throws Exception {
        given(currentMemberIdProvider.currentMemberId()).willReturn(1L);
        given(gameFacade.create(eq(1L), any())).willReturn(new CreateGameResponse(
                10L, 1L, "일요일 풋살", LocalDateTime.of(2026, 11, 1, 10, 0),
                "대구 풋살장", 12, 2, GameStatus.RECRUITING, false,
                LocalDateTime.of(2026, 10, 21, 14, 30)
        ));

        mockMvc.perform(post("/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.hostId").value(1))
                .andExpect(jsonPath("$.status").value("RECRUITING"))
                .andExpect(jsonPath("$.isDeleted").value(false));
    }

    @Test
    void 미인증_요청은_401을_반환한다() throws Exception {
        given(currentMemberIdProvider.currentMemberId()).willThrow(new FootyException(GlobalErrorCode.UNAUTHORIZED));

        mockMvc.perform(post("/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
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
