package com.tissue.member.application.service;

import com.tissue.member.application.port.out.MemberQueryRepository;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.MemberStatus;
import com.tissue.member.domain.exception.ActiveMemberNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberFinder {

    private final MemberQueryRepository memberRepository;

    public Member getActiveBy(Long memberId) {
        return memberRepository
                .findByIdAndStatus(memberId, MemberStatus.ACTIVE)
                .orElseThrow(() -> new ActiveMemberNotFoundException(memberId));
    }

    public Optional<Member> getOptActiveBy(Long memberId) {
        return memberRepository.findByIdAndStatus(memberId, MemberStatus.ACTIVE);
    }
}
