package com.tissue.feature.member.application.service;

import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.exception.ActiveMemberNotFoundException;
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

    public Optional<Member> getActiveByEmail(String email) {
        return memberRepository.findByEmailAndStatus(email, MemberStatus.ACTIVE);
    }
}
