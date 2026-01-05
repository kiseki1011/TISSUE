package com.tissue.member.application.port.in;

import com.tissue.member.application.dto.request.SignupMemberCommand;
import com.tissue.member.application.dto.request.SignupOAuthMemberCommand;
import com.tissue.member.application.dto.response.MemberSignupResponse;
import com.tissue.security.authentication.presentation.dto.response.OAuthSignupResponse;

public interface MemberCommandUseCase {

    MemberSignupResponse signup(SignupMemberCommand cmd);

    OAuthSignupResponse signupOAuth(SignupOAuthMemberCommand cmd);

    void linkOAuthAccount(String registerToken, Long memberId);

    void updateName(String name, Long memberId);

    void updateEmail(String newEmail, Long memberId);

    void updateUsername(String newUsername, Long memberId);

    void updatePassword(String originalPassword, String newPassword, Long memberId);

    void withdraw(String password, Long memberId);
}
