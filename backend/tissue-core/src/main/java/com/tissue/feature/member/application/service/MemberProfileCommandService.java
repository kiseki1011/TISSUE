package com.tissue.feature.member.application.service;

import com.tissue.feature.member.application.port.usecase.MemberProfileCommandUseCase;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.organization.position.application.service.finder.PositionFinder;
import com.tissue.feature.organization.position.domain.Position;
import com.tissue.shared.enums.SupportedLanguage;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberProfileCommandService implements MemberProfileCommandUseCase {

    private final MemberFinder memberFinder;
    private final PositionFinder positionFinder;

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
    public void updatePosition(@Nullable Long positionId, Long memberId) {
        Member member = memberFinder.getActiveById(memberId);

        Position position = positionId == null ? null : positionFinder.getById(positionId);
        member.assignPosition(position);
    }
}
