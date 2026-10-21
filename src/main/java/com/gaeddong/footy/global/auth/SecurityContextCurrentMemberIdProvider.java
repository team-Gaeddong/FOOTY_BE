package com.gaeddong.footy.global.auth;

import com.gaeddong.footy.global.error.FootyException;
import com.gaeddong.footy.global.error.GlobalErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentMemberIdProvider implements CurrentMemberIdProvider {

    @Override
    public Long currentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long memberId)) {
            throw new FootyException(GlobalErrorCode.UNAUTHORIZED);
        }
        return memberId;
    }
}
