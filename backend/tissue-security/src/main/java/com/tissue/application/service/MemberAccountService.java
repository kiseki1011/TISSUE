package com.tissue.application.service;

import com.tissue.application.port.usecase.MemberAccountUseCase;
import com.tissue.domain.exception.EmailNotVerifiedException;
import com.tissue.feature.member.application.port.out.AuthIdentityRepository;
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.application.service.MemberValidator;
import com.tissue.feature.member.domain.AuthIdentity;
import com.tissue.feature.member.domain.AuthProvider;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.exception.DuplicateEmailException;
import com.tissue.feature.member.domain.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberAccountService implements MemberAccountUseCase {

    private final MemberFinder memberFinder;
    private final AuthIdentityRepository authIdentityRepository;
    private final MemberValidator memberValidator;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final MemberEmailVerificationService memberEmailVerificationService;

    @Override
    public void addPassword(String newPassword, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        if (authIdentityRepository
                .findByProviderAndIdentifier(AuthProvider.EMAIL, member.getEmail())
                .isPresent()) {
            throw new IllegalArgumentException("Password already exists. Use update password instead.");
        }

        AuthIdentity emailIdentity =
                AuthIdentity.createEmailIdentity(member, member.getEmail(), passwordEncoder.encode(newPassword));

        authIdentityRepository.save(emailIdentity);
    }

    @Override
    public void updateEmail(String newEmail, String verificationToken, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);
        String oldEmail = member.getEmail();

        memberValidator.ensureUniqueEmail(newEmail);

        if (!memberEmailVerificationService.validateSignupToken(newEmail, verificationToken)) {
            throw new EmailNotVerifiedException(newEmail);
        }

        try {
            member.updateEmail(newEmail);

            authIdentityRepository
                    .findByProviderAndIdentifier(AuthProvider.EMAIL, oldEmail)
                    .ifPresent(identity -> identity.updateIdentifier(newEmail));

        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException(newEmail, e);
        }
    }

    @Override
    public void updatePassword(String originalPassword, String newPassword, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(member.getEmail(), originalPassword));

        AuthIdentity authIdentity = authIdentityRepository
                .findByProviderAndIdentifier(AuthProvider.EMAIL, member.getEmail())
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        authIdentity.updateCredential(passwordEncoder.encode(newPassword));
    }

    @Override
    public void withdraw(String password, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(member.getEmail(), password));
        memberValidator.ensureWithdrawable(member);

        member.withdraw();
    }
}
