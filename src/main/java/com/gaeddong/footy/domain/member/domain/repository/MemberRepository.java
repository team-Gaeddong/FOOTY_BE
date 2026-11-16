package com.gaeddong.footy.domain.member.domain.repository;

import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByProviderAndProviderId(Provider provider, String providerId);

    boolean existsByEmail(String email);

    // 탈퇴한 회원의 닉네임도 포함한다(D-05e)
    boolean existsByNickname(String nickname);

    boolean existsByIdAndOnboardedFalse(Long id);
}
