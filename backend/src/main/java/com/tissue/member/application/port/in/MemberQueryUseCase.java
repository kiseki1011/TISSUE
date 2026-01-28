package com.tissue.member.application.port.in;

import com.tissue.member.application.dto.response.GetMemberProfile;

public interface MemberQueryUseCase {

    GetMemberProfile getMyProfile(Long memberId);

    void checkEmailAvailability(String email);

    void checkUsernameAvailability(String username);
}
