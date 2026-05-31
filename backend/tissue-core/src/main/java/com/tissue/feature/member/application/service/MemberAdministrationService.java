package com.tissue.feature.member.application.service;

import com.tissue.feature.member.application.port.usecase.MemberAdministrationUseCase;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.organization.team.application.service.finder.TeamFinder;
import com.tissue.feature.organization.team.domain.Team;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberAdministrationService implements MemberAdministrationUseCase {

    private final MemberFinder memberFinder;
    private final TeamFinder teamFinder;

    @Override
    public void assignTeam(Long targetMemberId, @Nullable Long teamId, Long actorMemberId) {
        Member target = memberFinder.getActiveById(targetMemberId);

        Team team = teamId == null ? null : teamFinder.getById(teamId);
        target.assignTeam(team);
    }
}
