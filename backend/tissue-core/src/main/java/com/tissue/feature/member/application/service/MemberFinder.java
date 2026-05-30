package com.tissue.feature.member.application.service;

import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.exception.MemberDeletedException;
import com.tissue.feature.member.domain.exception.MemberNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberFinder {

    private final MemberQueryRepository memberRepository;

    public Member getActiveById(Long memberId) {
        Member member = getById(memberId);
        if (member.isDeleted()) {
            throw new MemberDeletedException(memberId);
        }
        return member;
    }

    public Optional<Member> getOptionalActiveById(Long memberId) {
        return memberRepository.findByIdAndStatus(memberId, MemberStatus.ACTIVE);
    }

    public Optional<Member> getActiveByEmail(String email) {
        return memberRepository.findByEmailAndStatus(email, MemberStatus.ACTIVE);
    }

    public List<Member> getAllActiveByIds(Set<Long> memberIds) {
        return memberRepository.findAllByIdInAndStatus(memberIds, MemberStatus.ACTIVE);
    }

    private Member getById(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new MemberNotFoundException(memberId));
    }
}
