package com.gaeddong.footy.global.security;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CORS_ALLOWED_ORIGINS를 비워 둔 운영 기본값에서는 다른 origin을 허용하지 않는다.
 */
@SpringBootTest(properties = "cors.allowed-origins=")
@AutoConfigureMockMvc
class CorsDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    @Test
    void 허용_origin이_비어_있으면_어떤_preflight도_허용하지_않는다() throws Exception {
        mockMvc.perform(options("/games")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
