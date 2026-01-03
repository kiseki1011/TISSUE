package com.tissue.member.application.service;

import com.tissue.member.application.dto.request.SignupMemberCommand;
import com.tissue.member.application.dto.response.MemberSignupResponse;
import com.tissue.member.application.port.in.MemberCommandUseCase;
import com.tissue.member.application.port.out.AuthIdentityRepository;
import com.tissue.member.application.port.out.MemberCommandRepository;
import com.tissue.member.application.service.finder.MemberFinder;
import com.tissue.member.application.service.validator.MemberValidator;
import com.tissue.member.domain.AuthIdentity;
import com.tissue.member.domain.AuthProvider;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.creator.AuthIdentityManager;
import com.tissue.member.domain.exception.MemberExceptions;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 관련 변경 작업(가입, 수정, 탈퇴)을 처리하는 서비스입니다.
 *
 * <p>
 * <b>주요 변경 사항 (AuthIdentity 도입):</b><br>
 * 비밀번호는 이제 `Member` 엔티티가 아닌 `AuthIdentity` 엔티티에서 관리됩니다.
 * 따라서 회원가입 시 `Member`와 `AuthIdentity`를 함께 생성하며, 비밀번호 변경 시 `AuthIdentity`를 수정합니다.
 * </p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class MemberCommandService implements MemberCommandUseCase {

    private final MemberFinder memberFinder;
    private final MemberCommandRepository memberCommandRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final AuthIdentityManager authIdentityManager;
    private final MemberValidator memberValidator;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final MemberEmailVerificationService memberEmailVerificationService;

    @Override
    public MemberSignupResponse signup(SignupMemberCommand cmd) {
        memberValidator.ensureUniqueEmail(cmd.email());
        memberValidator.ensureUniqueUsername(cmd.username());

        if (!memberEmailVerificationService.isEmailVerified(cmd.email())) {
            throw MemberExceptions.emailNotVerified(cmd.email());
        }

        Member member = Member.create(cmd.email(), cmd.username(), cmd.name());

        try {
            Member savedMember = memberCommandRepository.save(member);

            AuthIdentity authIdentity =
                    authIdentityManager.create(savedMember, cmd.provider(), cmd.email(), cmd.password());
            authIdentityRepository.save(authIdentity);

            memberEmailVerificationService.clearVerification(cmd.email());
            return MemberSignupResponse.from(savedMember);

        } catch (DataIntegrityViolationException e) {
            throw MemberExceptions.signUpConflict(cmd.email(), cmd.username(), e);
        }
    }

    @Override
    public void updateName(String name, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);
        member.updateName(name);
    }

    @Override
    public void updateEmail(String newEmail, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);
        String oldEmail = member.getEmail();

        memberValidator.ensureUniqueEmail(newEmail);

        if (!memberEmailVerificationService.isEmailVerified(newEmail)) {
            throw MemberExceptions.emailNotVerified(newEmail);
        }

        try {
            member.updateEmail(newEmail);

            authIdentityRepository
                    .findByProviderAndIdentifier(AuthProvider.EMAIL, oldEmail)
                    .ifPresent(identity -> identity.updateIdentifier(newEmail));

            memberEmailVerificationService.clearVerification(newEmail);
        } catch (DataIntegrityViolationException e) {
            throw MemberExceptions.duplicateEmail(newEmail, e);
        }
    }

    @Override
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
    public void updatePassword(String originalPassword, String newPassword, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(member.getEmail(), originalPassword));

        AuthIdentity authIdentity = authIdentityRepository
                .findByProviderAndIdentifier(AuthProvider.EMAIL, member.getEmail())
                .orElseThrow(() -> MemberExceptions.notFound(memberId)); // 엄밀히는 Identity not found

        authIdentity.updateCredential(passwordEncoder.encode(newPassword));
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
