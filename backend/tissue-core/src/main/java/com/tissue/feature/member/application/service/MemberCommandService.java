package com.tissue.feature.member.application.service;

import com.tissue.feature.member.application.port.in.MemberCommandUseCase;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.exception.DuplicateUsernameException;
import com.tissue.shared.enums.SupportedLanguage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberCommandService implements MemberCommandUseCase {

    private final MemberFinder memberFinder;
    private final MemberValidator memberValidator;

    @Override
    public void updateName(String name, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);
        member.updateName(name);
    }

    @Override
    public void updateLanguage(SupportedLanguage language, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);
        member.updateLanguage(language);
    }

    @Override
    public void updateUsername(String newUsername, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        memberValidator.ensureUniqueUsername(newUsername);

        try {
            member.updateUsername(newUsername);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateUsernameException(newUsername, e);
        }
    }
}
