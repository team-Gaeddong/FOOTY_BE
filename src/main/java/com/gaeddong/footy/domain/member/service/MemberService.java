package com.gaeddong.footy.domain.member.service;

import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.repository.MemberPositionRepository;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.domain.member.exception.MemberErrorCode;
import com.gaeddong.footy.domain.member.presentation.dto.MyProfileResponse;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberPositionRepository memberPositionRepository;

    // 탈퇴한 회원은 없는 회원으로 본다
    public MyProfileResponse getMyProfile(Long memberId) {
        Member member = memberRepository.findByIdAndDeletedFalse(memberId)
                .orElseThrow(() -> new FootyException(MemberErrorCode.MEMBER_NOT_FOUND));
        return MyProfileResponse.of(member, memberPositionRepository.findAllByMemberIdOrderByPriorityAsc(memberId));
    }
}
