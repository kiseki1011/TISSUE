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
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberQueryService implements MemberQueryUseCase {

    private final MemberFinder memberFinder;
    private final MemberValidator memberValidator;

    @Override
    public GetMemberProfile getMyProfile(Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);
        return GetMemberProfile.from(member);
    }

    @Override
    public void checkEmailAvailability(String email) {
        memberValidator.ensureUniqueEmail(email);
    }

    @Override
    public void checkUsernameAvailability(String username) {
        memberValidator.ensureUniqueUsername(username);
    }
}
