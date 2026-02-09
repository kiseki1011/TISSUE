package com.tissue.member.application.port.in;

import com.tissue.authentication.application.dto.response.OAuthSignupResponse;
import com.tissue.enums.SupportedLanguage;
import com.tissue.member.application.dto.request.SignupMemberCommand;
import com.tissue.member.application.dto.request.SignupOAuthMemberCommand;
import com.tissue.member.application.dto.response.MemberSignupResponse;

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
