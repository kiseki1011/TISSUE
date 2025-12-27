package com.tissue.member.application.port.in;

import com.tissue.member.application.dto.response.GetMemberProfile;

public interface MemberQueryUseCase {

    GetMemberProfile getMyProfile(Long memberId);

    /** Checks if the email is available (unique). Throws exception if duplicate. */
    void checkEmailAvailability(String email);

    /** Checks if the username is available (unique). Throws exception if duplicate. */
    void checkUsernameAvailability(String username);
}
