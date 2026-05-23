package com.tissue.feature.member.application.port.usecase;

import com.tissue.feature.member.application.dto.MemberProfile;

public interface MemberProfileQueryUseCase {

    MemberProfile getMyProfile(Long memberId);
}
