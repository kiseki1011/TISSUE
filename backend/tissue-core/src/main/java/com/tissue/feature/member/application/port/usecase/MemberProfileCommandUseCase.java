package com.tissue.feature.member.application.port.usecase;

import com.tissue.shared.enums.SupportedLanguage;
import org.jspecify.annotations.Nullable;

public interface MemberProfileCommandUseCase {

    void updateName(String name, Long memberId);

    void updateLanguage(SupportedLanguage language, Long memberId);

    void updatePosition(@Nullable Long positionId, Long memberId);
}
