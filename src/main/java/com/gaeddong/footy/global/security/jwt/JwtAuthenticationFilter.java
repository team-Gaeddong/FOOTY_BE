package com.gaeddong.footy.global.security.jwt;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.auth.exception.AuthErrorCode;
import com.gaeddong.footy.global.error.ErrorResponseWriter;
import com.gaeddong.footy.global.error.FootyException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorization: Bearer {access 토큰}을 검증해 회원 ID를 principal로, 토큰을 credentials로 둔다.
 * 토큰이 있는데 유효하지 않으면 바로 401을 응답한다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;
    private final ErrorResponseWriter errorResponseWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        try {
            Long memberId = jwtProvider.getMemberId(token, TokenType.ACCESS);
            if (accessTokenBlacklistRepository.contains(token)) {
                throw new FootyException(AuthErrorCode.LOGGED_OUT_TOKEN);
            }
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(memberId, token, List.of()));
        } catch (FootyException e) {
            SecurityContextHolder.clearContext();
            errorResponseWriter.write(response, e.getErrorCode());
            return;
        }
        filterChain.doFilter(request, response);
    }
}
