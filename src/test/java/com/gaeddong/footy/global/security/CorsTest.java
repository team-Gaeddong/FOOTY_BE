package com.gaeddong.footy.global.security;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 테스트 설정의 허용 origin은 http://localhost:5173이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsTest {

    private static final String ALLOWED = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    @Test
    void 허용된_origin의_preflight는_CORS_헤더와_함께_통과한다() throws Exception {
        mockMvc.perform(options("/games/1/teams/1/tactic")
                        .header("Origin", ALLOWED)
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    void 허용되지_않은_origin의_preflight는_403이다() throws Exception {
        mockMvc.perform(options("/games")
                        .header("Origin", "http://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void 허용되지_않은_헤더를_요청하는_preflight는_403이다() throws Exception {
        mockMvc.perform(options("/games")
                        .header("Origin", ALLOWED)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "X-Custom"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 인증_실패_응답에도_CORS_헤더가_있어_프론트가_에러를_읽을_수_있다() throws Exception {
        mockMvc.perform(get("/games").header("Origin", ALLOWED))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED));
    }
}
