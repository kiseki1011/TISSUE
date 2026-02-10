package com.tissue.feature.member.application.port.in;

import com.tissue.feature.authentication.application.dto.response.OAuthSignupResponse;
import com.tissue.feature.member.application.dto.request.SignupMemberCommand;
import com.tissue.feature.member.application.dto.request.SignupOAuthMemberCommand;
import com.tissue.feature.member.application.dto.response.MemberSignupResponse;
import com.tissue.shared.enums.SupportedLanguage;

public interface MemberCommandUseCase {

    MemberSignupResponse signup(SignupMemberCommand command);

    OAuthSignupResponse signupOAuth(SignupOAuthMemberCommand command);

    void linkOAuthAccount(String registerToken, Long memberId);

    void addPassword(String newPassword, Long memberId);

    void updateName(String name, Long memberId);

    void updateLanguage(SupportedLanguage language, Long memberId);

    void updateEmail(String newEmail, String verificationToken, Long memberId);

    void updateUsername(String newUsername, Long memberId);

    void updatePassword(String originalPassword, String newPassword, Long memberId);

    void withdraw(String password, Long memberId);
}
