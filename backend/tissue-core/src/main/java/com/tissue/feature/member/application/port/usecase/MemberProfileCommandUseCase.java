package com.tissue.feature.member.application.port.usecase;

import com.tissue.shared.enums.SupportedLanguage;

public interface MemberProfileCommandUseCase {

    void updateName(String name, Long memberId);

    void updateLanguage(SupportedLanguage language, Long memberId);
}
