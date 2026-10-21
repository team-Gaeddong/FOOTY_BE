package com.gaeddong.footy.global.security.onboarding;

import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.domain.member.exception.MemberErrorCode;
import com.gaeddong.footy.global.error.ErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 온보딩을 마치지 않은 회원은 아래 API만 쓸 수 있고, 나머지는 403으로 막는다(D-05e).
 * 토큰 재발급은 인증 없이 열려 있어 여기서 검사하지 않는다. 탈퇴 API를 추가하면 허용 목록에 넣는다.
 * 온보딩 직후 바로 풀리도록 토큰이 아닌 DB의 is_onboarded를 확인한다.
 */
@RequiredArgsConstructor
public class OnboardingFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_REQUESTS = Set.of(
            "PATCH /members/me/onboarding",
            "POST /auth/logout");

    private final MemberRepository memberRepository;
    private final ErrorResponseWriter errorResponseWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long memberId
                && !isAllowed(request)
                && memberRepository.existsByIdAndOnboardedFalse(memberId)) {
            errorResponseWriter.write(response, MemberErrorCode.ONBOARDING_REQUIRED);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return ALLOWED_REQUESTS.contains(request.getMethod() + " " + path);
    }
}
