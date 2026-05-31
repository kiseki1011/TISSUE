package com.tissue.feature.organization.team.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.organization.team.application.dto.response.TeamSummary;
import com.tissue.feature.organization.team.application.port.repository.TeamRepository;
import com.tissue.feature.organization.team.application.port.usecase.TeamQueryUseCase;
import com.tissue.feature.organization.team.domain.Team;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TeamQueryService implements TeamQueryUseCase {

    private final TeamRepository teamRepository;
    private final MemberFinder memberFinder;

    @Override
    public List<TeamSummary> getTeams(Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        List<Team> teams = teamRepository.findAllOrderById();

        return teams.stream().map(TeamSummary::from).toList();
    }
}
