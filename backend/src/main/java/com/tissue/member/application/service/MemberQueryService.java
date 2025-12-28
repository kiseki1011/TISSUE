package com.tissue.member.application.service;

import com.tissue.member.application.dto.response.GetMemberProfile;
import com.tissue.member.application.port.in.MemberQueryUseCase;
import com.tissue.member.application.service.finder.MemberFinder;
import com.tissue.member.application.service.validator.MemberValidator;
import com.tissue.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberQueryService implements MemberQueryUseCase {

    private final MemberFinder memberFinder;
    private final MemberValidator memberValidator;

    @Override
    @Transactional(readOnly = true)
    public GetMemberProfile getMyProfile(Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);
        return GetMemberProfile.from(member);
    }

    @Override
    @Transactional(readOnly = true)
    public void checkEmailAvailability(String email) {
        // Refactored: Delegated from Controller to Service to keep Controller clean.
        memberValidator.ensureUniqueEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public void checkUsernameAvailability(String username) {
        memberValidator.ensureUniqueUsername(username);
    }
}
