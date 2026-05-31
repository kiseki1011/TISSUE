package com.tissue.feature.member.application.port.usecase;

import org.jspecify.annotations.Nullable;

public interface MemberAdministrationUseCase {

    void assignTeam(Long targetMemberId, @Nullable Long teamId, Long actorMemberId);
}
