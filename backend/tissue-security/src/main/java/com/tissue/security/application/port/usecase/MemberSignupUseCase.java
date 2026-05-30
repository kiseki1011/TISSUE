package com.tissue.security.application.port.usecase;

import com.tissue.security.application.dto.command.SignupMemberCommand;
import com.tissue.security.application.dto.response.MemberSignupResponse;

public interface MemberSignupUseCase {

    MemberSignupResponse signup(SignupMemberCommand command);
}
