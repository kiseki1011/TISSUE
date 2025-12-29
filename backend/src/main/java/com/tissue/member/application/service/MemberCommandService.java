package com.tissue.member.application.service;

import com.tissue.member.application.dto.request.SignupMemberCommand;
import com.tissue.member.application.dto.response.MemberSignupResponse;
import com.tissue.member.application.port.in.MemberCommandUseCase;
import com.tissue.member.application.port.out.MemberCommandRepository;
import com.tissue.member.application.service.finder.MemberFinder;
import com.tissue.member.application.service.validator.MemberValidator;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.exception.MemberExceptions;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberCommandService implements MemberCommandUseCase {

    private final MemberFinder memberFinder;
    private final MemberCommandRepository memberCommandRepository;
    private final MemberValidator memberValidator;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final MemberEmailVerificationService memberEmailVerificationService;

    @Override
    @Transactional
    public MemberSignupResponse signup(SignupMemberCommand cmd) {
        memberValidator.ensureUniqueEmail(cmd.email());
        memberValidator.ensureUniqueUsername(cmd.username());

        if (!memberEmailVerificationService.isEmailVerified(cmd.email())) {
            throw MemberExceptions.emailNotVerified(cmd.email());
        }

        Member member = Member.create(cmd.email(), cmd.username(), passwordEncoder.encode(cmd.password()), cmd.name());

        try {
            Member savedMember = memberCommandRepository.save(member);
            memberEmailVerificationService.clearVerification(cmd.email());
            return MemberSignupResponse.from(savedMember);
        } catch (DataIntegrityViolationException e) {
            throw MemberExceptions.signUpConflict(cmd.email(), cmd.username(), e);
        }
    }

    @Override
    @Transactional
    public void updateName(String name, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);
        member.updateName(name);
    }

    @Override
    @Transactional
    public void updateEmail(String newEmail, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        memberValidator.ensureUniqueEmail(newEmail);

        if (!memberEmailVerificationService.isEmailVerified(newEmail)) {
            throw MemberExceptions.emailNotVerified(newEmail);
        }

        try {
            member.updateEmail(newEmail);
            memberEmailVerificationService.clearVerification(newEmail);
        } catch (DataIntegrityViolationException e) {
            throw MemberExceptions.duplicateEmail(newEmail, e);
        }
    }

    @Override
    @Transactional
    public void updateUsername(String newUsername, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        memberValidator.ensureUniqueUsername(newUsername);

        try {
            member.updateUsername(newUsername);
        } catch (DataIntegrityViolationException e) {
            throw MemberExceptions.duplicateUsername(newUsername, e);
        }
    }

    @Override
    @Transactional
    public void updatePassword(String originalPassword, String newPassword, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(member.getEmail(), originalPassword));

        member.updatePassword(passwordEncoder.encode(newPassword));
    }

    @Override
    @Transactional
    public void withdraw(String password, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(member.getEmail(), password));

        memberValidator.ensureWithdrawable(member);

        member.withdraw();
    }
}
