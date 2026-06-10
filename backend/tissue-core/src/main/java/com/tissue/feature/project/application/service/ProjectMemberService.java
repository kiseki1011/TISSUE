package com.tissue.feature.project.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.response.ProjectMemberResponse;
import com.tissue.feature.project.application.dto.response.ProjectMembersResponse;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.port.usecase.ProjectMemberUseCase;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectAccessResolver;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectMemberService implements ProjectMemberUseCase {

    private final ProjectFinder projectFinder;
    private final ProjectAccessResolver projectAccessResolver;
    private final ProjectMemberFinder projectMemberFinder;
    private final MemberFinder memberFinder;
    private final ProjectMemberCommandRepository projectMemberRepository;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final ProjectEventPublisher projectEventPublisher;
    private final AgentProjectJoinService agentProjectJoinService;

    @Override
    public ProjectMembersResponse addMembers(ProjectIdentifier pid, Set<Long> targetMemberIds, Long actorMemberId) {
        ProjectMember actor = projectAccessResolver.resolveByProjectKey(pid.projectKey(), actorMemberId);

        Project project = projectFinder.getByProjectKey(pid.projectKey());

        projectAuthorizationService.requireProjectManager(actor);

        List<Member> members = memberFinder.getAllActiveByIds(targetMemberIds);

        List<ProjectMember> added = new ArrayList<>();

        for (Member member : members) {
            Optional<ProjectMember> existing =
                    projectMemberFinder.findOptionalIncludingSoftDeleted(project, member.getId());

            if (existing.isEmpty()) {
                ProjectMember created = ProjectMember.create(project, member);
                projectMemberRepository.save(created);
                added.add(created);
            } else if (existing.get().isSoftDeleted()) {
                ProjectMember restored = restoreProjectMember(existing.get());
                added.add(restored);
            }
        }

        for (ProjectMember addedMember : added) {
            agentProjectJoinService.includeAgentsOfMember(addedMember.getMemberId(), project);
        }

        return ProjectMembersResponse.of(project, added);
    }

    @Override
    public ProjectMemberResponse join(ProjectIdentifier pid, Long actorMemberId) {
        Project project = projectFinder.getByProjectKey(pid.projectKey());

        Member actor = memberFinder.getActiveById(actorMemberId);
        projectAuthorizationService.requireJoinPermission(actor, project);

        Optional<ProjectMember> existing = projectMemberFinder.findOptionalIncludingSoftDeleted(project, actorMemberId);
        if (existing.isPresent()) {
            ProjectMember projectMember = existing.get();
            if (projectMember.isSoftDeleted()) {
                restoreProjectMember(projectMember);
                agentProjectJoinService.includeAgentsOfMember(actorMemberId, project);
            }
            return ProjectMemberResponse.of(projectMember);
        }

        ProjectMember projectMember = ProjectMember.create(project, actor);
        projectMemberRepository.save(projectMember);

        agentProjectJoinService.includeAgentsOfMember(actorMemberId, project);

        return ProjectMemberResponse.of(projectMember);
    }

    @Override
    public void changeRole(ProjectIdentifier pid, Long targetMemberId, ProjectRole role, Long actorMemberId) {
        ProjectMember actor = projectAccessResolver.resolveByProjectKey(pid.projectKey(), actorMemberId);
        ProjectMember target = projectMemberFinder.getWithProject(pid.projectKey(), targetMemberId);

        projectAuthorizationService.requireHigherRole(actor, target);

        ProjectRole oldRole = target.getRole();
        target.changeRole(role);

        if (oldRole != role) {
            projectEventPublisher.publishRoleChanged(target, oldRole, actor);
        }
    }

    @Override
    public void leave(ProjectIdentifier pid, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getWithProject(pid.projectKey(), actorMemberId);

        actor.softDelete();

        agentProjectJoinService.revokeAgentsOfMember(actorMemberId, actor.getProject());
    }

    @Override
    public void kickMember(ProjectIdentifier pid, Long targetMemberId, Long actorMemberId) {
        ProjectMember actor = projectAccessResolver.resolveByProjectKey(pid.projectKey(), actorMemberId);

        ensureNotSelfKick(targetMemberId, actorMemberId);

        ProjectMember target = projectMemberFinder.getWithProject(pid.projectKey(), targetMemberId);

        projectAuthorizationService.requireHigherRole(actor, target);

        target.softDelete();

        agentProjectJoinService.revokeAgentsOfMember(targetMemberId, target.getProject());
    }

    private ProjectMember restoreProjectMember(ProjectMember projectMember) {
        projectMember.ensureEditable();
        projectMember.restoreSoftDeleted();
        projectMember.changeRole(ProjectRole.MEMBER);

        return projectMember;
    }

    private void ensureNotSelfKick(Long targetMemberId, Long actorMemberId) {
        if (Objects.equals(actorMemberId, targetMemberId)) {
            throw new BadRequestException(ProjectErrorCode.SELF_KICK_NOT_ALLOWED);
        }
    }
}
