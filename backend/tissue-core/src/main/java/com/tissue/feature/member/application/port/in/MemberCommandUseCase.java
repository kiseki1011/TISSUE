package com.tissue.feature.member.application.port.in;

import com.tissue.shared.enums.SupportedLanguage;

public interface MemberCommandUseCase {

    void updateUsername(String newUsername, Long memberId);

    void updateName(String name, Long memberId);

    void updateLanguage(SupportedLanguage language, Long memberId);
}
