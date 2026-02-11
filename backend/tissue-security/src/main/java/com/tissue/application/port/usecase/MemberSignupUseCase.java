package com.tissue.application.port.usecase;

import com.tissue.application.dto.command.SignupMemberCommand;
import com.tissue.application.dto.command.SignupOAuthMemberCommand;
import com.tissue.application.dto.response.MemberSignupResponse;
import com.tissue.application.dto.response.OAuthSignupResponse;

public interface MemberSignupUseCase {

    MemberSignupResponse signup(SignupMemberCommand command);

    OAuthSignupResponse signupOAuth(SignupOAuthMemberCommand command);

    void linkOAuthAccount(String registerToken, Long memberId);
}
