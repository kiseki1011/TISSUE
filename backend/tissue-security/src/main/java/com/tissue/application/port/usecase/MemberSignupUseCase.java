package com.tissue.application.port.usecase;

import com.tissue.application.dto.response.OAuthSignupResponse;
import com.tissue.feature.member.application.dto.request.SignupMemberCommand;
import com.tissue.feature.member.application.dto.request.SignupOAuthMemberCommand;
import com.tissue.feature.member.application.dto.response.MemberSignupResponse;

public interface MemberSignupUseCase {

    MemberSignupResponse signup(SignupMemberCommand command);

    OAuthSignupResponse signupOAuth(SignupOAuthMemberCommand command);

    void linkOAuthAccount(String registerToken, Long memberId);
}
