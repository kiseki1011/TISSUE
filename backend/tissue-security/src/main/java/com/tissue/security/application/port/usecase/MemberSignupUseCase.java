package com.tissue.security.application.port.usecase;

import com.tissue.security.application.dto.command.SignupMemberCommand;
import com.tissue.security.application.dto.command.SignupOAuthMemberCommand;
import com.tissue.security.application.dto.response.MemberSignupResponse;
import com.tissue.security.application.dto.response.OAuthSignupResponse;

public interface MemberSignupUseCase {

    MemberSignupResponse signupWithEmail(SignupMemberCommand command);

    OAuthSignupResponse signupWithOAuth(SignupOAuthMemberCommand command);
}
