package com.tissue.security.application.service;

import static com.tissue.security.domain.exception.AuthenticationErrorCode.EMAIL_SIGNUP_DISABLED;

import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.security.config.SignupProperties;
import com.tissue.shared.exception.base.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignupGuardrails {

    private final MemberQueryRepository memberRepository;
    private final SignupProperties signupProperties;

    /**
     * The very first member to sign up on a fresh instance is promoted to {@code SUPER_ADMIN}.
     */
    public boolean isFirstUser() {
        return memberRepository.count() == 0;
    }

    public void ensureSignupAllowed() {
        if (isFirstUser()) {
            return;
        }
        if (!signupProperties.isEnabled()) {
            throw new ForbiddenException(EMAIL_SIGNUP_DISABLED);
        }
    }
}
