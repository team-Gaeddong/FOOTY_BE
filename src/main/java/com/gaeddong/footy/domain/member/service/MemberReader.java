package com.gaeddong.footy.domain.member.service;

import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.domain.member.exception.MemberErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 회원을 찾는 공통 규칙. 없거나 탈퇴한 회원은 도메인에 상관없이 404 MEMBER_404_1이다(D-16).
 * 트랜잭션은 호출하는 서비스의 것을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class MemberReader {

    private final MemberRepository memberRepository;

    public Member getActiveMember(Long memberId) {
        return memberRepository.findById(memberId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new FootyException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
