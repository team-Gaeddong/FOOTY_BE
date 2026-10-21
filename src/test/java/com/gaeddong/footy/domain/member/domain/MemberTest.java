package com.gaeddong.footy.domain.member.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 12, 0);

    @Test
    void 온보딩을_마치면_닉네임이_채워진다() {
        Member member = Member.signUp("player@gmail.com", Provider.GOOGLE, "google-sub-1", NOW);

        member.completeOnboarding("태균");

        assertThat(member.getNickname()).isEqualTo("태균");
        assertThat(member.isOnboarded()).isTrue();
    }

    @Test
    void 온보딩은_한_번만_할_수_있다() {
        Member member = Member.signUp("player@gmail.com", Provider.GOOGLE, "google-sub-1", NOW);
        member.completeOnboarding("태균");

        assertThatThrownBy(() -> member.completeOnboarding("다른닉네임"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 탈퇴하면_소프트_삭제된다() {
        Member member = Member.signUp("player@gmail.com", Provider.GOOGLE, "google-sub-1", NOW);

        member.withdraw(NOW.plusDays(1));

        assertThat(member.isDeleted()).isTrue();
        assertThat(member.getDeletedAt()).isEqualTo(NOW.plusDays(1));
        assertThatThrownBy(() -> member.withdraw(NOW.plusDays(2)))
                .isInstanceOf(IllegalStateException.class);
    }
}
