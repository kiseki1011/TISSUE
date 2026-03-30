package com.tissue.feature.member.application.service;

import com.tissue.feature.member.application.dto.MemberProfile;
import com.tissue.feature.member.application.port.usecase.MemberProfileUseCase;
import com.tissue.feature.member.domain.Member;
import com.tissue.shared.enums.SupportedLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberProfileService implements MemberProfileUseCase {

    private final MemberFinder memberFinder;

    @Override
    public void updateName(String name, Long memberId) {
        Member member = memberFinder.getActiveById(memberId);
        member.updateName(name);
    }

    @Override
    public void updateLanguage(SupportedLanguage language, Long memberId) {
        Member member = memberFinder.getActiveById(memberId);
        member.updateLanguage(language);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberProfile getMyProfile(Long memberId) {
        Member member = memberFinder.getActiveById(memberId);
        return MemberProfile.from(member);
    }
}
