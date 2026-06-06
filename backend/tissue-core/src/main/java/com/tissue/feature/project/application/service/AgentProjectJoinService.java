package com.tissue.feature.project.application.service;

import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AgentProjectJoinService {

    private final MemberFinder memberFinder;
    private final MemberQueryRepository memberQueryRepository;
    private final ProjectMemberQueryRepository projectMemberQueryRepository;
    private final ProjectMemberCommandRepository projectMemberRepository;

    /**
     * Include agent ({@code agentMemberId}) into every active, non-archived project its owner
     * currently belongs to. Called when an agent is first created.
     */
    public void includeAgentIntoOwnerProjects(Long agentMemberId) {
        Member agent = memberFinder.getActiveById(agentMemberId);
        Member owner = agent.getOwner();
        if (owner == null) {
            return;
        }
        for (ProjectMember ownerMembership : projectMemberQueryRepository.findAllWithProjectByMemberId(owner.getId())) {
            join(ownerMembership.getProject(), agent);
        }
    }

    /**
     * Include every active agent owned by {@code humanMemberId} into {@code project}. Called when the
     * human joins / added to / creates the project.
     */
    public void includeAgentsOfMember(Long humanMemberId, Project project) {
        if (project.isArchived()) {
            return;
        }
        for (Member agent : memberQueryRepository.findAllByOwner_IdAndStatus(humanMemberId, MemberStatus.ACTIVE)) {
            join(project, agent);
        }
    }

    /**
     * Revoke the memberships of a user's ({@code humanMemberId}) agents in {@code project}.
     * Called when the owner leaves / kicked from the project.
     */
    public void revokeAgentsOfMember(Long humanMemberId, Project project) {
        for (Member agent : memberQueryRepository.findAllByOwner_IdAndStatus(humanMemberId, MemberStatus.ACTIVE)) {
            projectMemberQueryRepository
                    .findByProjectAndMemberId(project, agent.getId())
                    .ifPresent(ProjectMember::softDelete);
        }
    }

    private void join(Project project, Member agent) {
        if (project.isArchived()) {
            return;
        }
        projectMemberQueryRepository
                .findByProjectAndMemberIdIncludingSoftDeleted(project, agent.getId())
                .ifPresentOrElse(
                        ProjectMember::restoreSoftDeleted,
                        () -> projectMemberRepository.save(ProjectMember.create(project, agent)));
    }
}
