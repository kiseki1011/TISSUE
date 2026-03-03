package com.tissue.feature.member.application.port.usecase;

import com.tissue.feature.member.application.dto.MemberProfile;
import com.tissue.shared.enums.SupportedLanguage;

public interface MemberProfileUseCase {

    void updateName(String name, Long memberId);

    void updateLanguage(SupportedLanguage language, Long memberId);

    MemberProfile getMyProfile(Long memberId);
}
