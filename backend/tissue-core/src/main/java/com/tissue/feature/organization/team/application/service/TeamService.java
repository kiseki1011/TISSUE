package com.tissue.feature.organization.team.application.service;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.organization.team.application.dto.request.CreateTeamCommand;
import com.tissue.feature.organization.team.application.dto.request.PatchTeamCommand;
import com.tissue.feature.organization.team.application.dto.response.TeamResponse;
import com.tissue.feature.organization.team.application.port.repository.TeamRepository;
import com.tissue.feature.organization.team.application.port.usecase.TeamUseCase;
import com.tissue.feature.organization.team.application.service.finder.TeamFinder;
import com.tissue.feature.organization.team.application.service.validator.TeamValidator;
import com.tissue.feature.organization.team.domain.Team;
import com.tissue.shared.vo.Name;
import com.tissue.support.util.Patchers;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TeamService implements TeamUseCase {

    private final TeamFinder teamFinder;
    private final TeamRepository teamRepository;
    private final TeamValidator teamValidator;
    private final MemberCommandRepository memberCommandRepository;

    @Override
    public TeamResponse create(CreateTeamCommand cmd, Long actorMemberId) {
        teamValidator.ensureUniqueLabel(cmd.name());

        Team team = Team.create(cmd.name(), cmd.description(), cmd.color());

        Team saved = teamRepository.save(team);

        return TeamResponse.from(saved);
    }

    @Override
    public void update(Long teamId, PatchTeamCommand cmd, Long actorMemberId) {
        Team team = teamFinder.getById(teamId);

        Patchers.apply(cmd.name(), newName -> {
            Name name = Name.of(newName);
            if (!isNameUnchanged(team, name)) {
                teamValidator.ensureUniqueLabel(name);
                team.rename(name);
            }
        });
        Patchers.apply(cmd.description(), team::updateDescription);
        Patchers.apply(cmd.color(), team::updateColor);
    }

    @Override
    public void delete(Long teamId, Long actorMemberId) {
        Team team = teamFinder.getById(teamId);

        memberCommandRepository.clearTeamAssignments(team);

        teamRepository.delete(team);
    }

    private boolean isNameUnchanged(Team team, Name newName) {
        return Objects.equals(team.getName(), newName.toString());
    }
}
