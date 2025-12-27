package com.tissue.member.application.port.in;

import com.tissue.member.application.dto.request.SignupMemberCommand;
import com.tissue.member.application.dto.response.MemberSignupResponse;

public interface MemberCommandUseCase {

	MemberSignupResponse signup(SignupMemberCommand cmd);

	// TODO: should i change method names to updateMyXxx?
	//  for example: updateName -> updateMyName
	void updateName(String name, Long memberId);

	void updateEmail(String newEmail, Long memberId);

	void updateUsername(String newUsername, Long memberId);

	void updatePassword(String originalPassword, String newPassword, Long memberId);

	void withdraw(String password, Long memberId);
}
