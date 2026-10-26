package com.gaeddong.footy.domain.member.facade;

import com.gaeddong.footy.domain.auth.service.AuthService;
import com.gaeddong.footy.domain.member.service.MemberService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.redis.RedisConnectionFailureException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MemberFacadeTest {

    private final MemberService memberService = mock(MemberService.class);
    private final AuthService authService = mock(AuthService.class);
    private final MemberFacade memberFacade = new MemberFacade(memberService, authService);

    @Test
    void 토큰을_먼저_무효화한_뒤_탈퇴한다() {
        memberFacade.withdraw(1L, "access");

        InOrder order = inOrder(authService, memberService);
        order.verify(authService).logout(1L, "access");
        order.verify(memberService).withdraw(1L);
    }

    @Test
    void 토큰_무효화가_실패하면_탈퇴하지_않는다() {
        willThrow(new RedisConnectionFailureException("redis down")).given(authService).logout(1L, "access");

        assertThatThrownBy(() -> memberFacade.withdraw(1L, "access"))
                .isInstanceOf(RedisConnectionFailureException.class);
        verify(memberService, never()).withdraw(anyLong());
    }
}
