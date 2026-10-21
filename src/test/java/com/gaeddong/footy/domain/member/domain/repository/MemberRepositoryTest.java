package com.gaeddong.footy.domain.member.domain.repository;

import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class MemberRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 12, 0);

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 가입한_회원을_제공자_식별자로_찾는다() {
        memberRepository.saveAndFlush(Member.signUp("player@gmail.com", Provider.GOOGLE, "google-sub-1", NOW));

        Member found = memberRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-sub-1").orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getEmail()).isEqualTo("player@gmail.com");
        assertThat(found.getNickname()).isNull();
        assertThat(found.getRating()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(found.isOnboarded()).isFalse();
        assertThat(found.isDeleted()).isFalse();
        assertThat(memberRepository.existsByEmail("player@gmail.com")).isTrue();
    }

    @Test
    void 같은_구글_계정으로_두_번_가입할_수_없다() {
        memberRepository.saveAndFlush(Member.signUp("a@gmail.com", Provider.GOOGLE, "google-sub-1", NOW));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(
                Member.signUp("b@gmail.com", Provider.GOOGLE, "google-sub-1", NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 같은_이메일로_두_번_가입할_수_없다() {
        memberRepository.saveAndFlush(Member.signUp("a@gmail.com", Provider.GOOGLE, "google-sub-1", NOW));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(
                Member.signUp("a@gmail.com", Provider.GOOGLE, "google-sub-2", NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
