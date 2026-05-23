package com.tissue.feature.member.application.service;

import com.tissue.feature.member.application.dto.MemberProfile;
import com.tissue.feature.member.application.port.usecase.MemberProfileQueryUseCase;
import com.tissue.feature.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberProfileQueryService implements MemberProfileQueryUseCase {

    private final MemberFinder memberFinder;

    @Override
    public MemberProfile getMyProfile(Long memberId) {
        Member member = memberFinder.getActiveById(memberId);
        return MemberProfile.from(member);
    }
}
