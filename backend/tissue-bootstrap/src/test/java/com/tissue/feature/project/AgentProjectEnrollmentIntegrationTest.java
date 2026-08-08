package com.tissue.feature.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.agent.application.dto.AgentResponse;
import com.tissue.feature.agent.application.dto.CreateAgentCommand;
import com.tissue.feature.agent.application.service.AgentService;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.request.CreateProjectCommand;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.application.service.AgentProjectJoinService;
import com.tissue.feature.project.application.service.ProjectMemberService;
import com.tissue.feature.project.application.service.ProjectService;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.support.IntegrationTestSupport;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AgentProjectEnrollmentIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AgentService agentService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectMemberService projectMemberService;

    @Autowired
    private AgentProjectJoinService agentProjectJoinService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private ProjectCommandRepository projectCommandRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberCommandRepository;

    @Autowired
    private ProjectMemberQueryRepository projectMemberQueryRepository;

    private Member newHuman(String username) {
        return memberCommandRepository.save(Member.create(username + "@tissue.dev", username, "Human"));
    }

    private Project newProject(String key) {
        return projectCommandRepository.save(Project.create(key, key + " title", null));
    }

    private void addAsMember(Project project, Member member) {
        projectMemberCommandRepository.save(ProjectMember.create(project, member));
    }

    private void addAsManager(Project project, Member member) {
        projectMemberCommandRepository.save(ProjectMember.createManager(project, member));
    }

    @Test
    @DisplayName("success: creating an agent includes it into every project its owner already belongs to")
    void backfillsAgentIntoOwnersExistingProjects() {
        // given
        Member owner = newHuman("owner");
        Project proj1 = newProject("PROJ1");
        Project proj2 = newProject("PROJ2");
        addAsMember(proj1, owner);
        addAsMember(proj2, owner);

        // when
        AgentResponse agent = agentService.createAgent(
                owner.getId(), CreateAgentCommand.builder().name("Bot").build());

        // then
        assertThat(projectMemberQueryRepository.findByProjectAndMemberId(proj1, agent.id()))
                .get()
                .extracting(ProjectMember::getRole)
                .isEqualTo(ProjectRole.MEMBER);
        assertThat(projectMemberQueryRepository.findByProjectAndMemberId(proj2, agent.id()))
                .isPresent();
    }

    @Test
    @DisplayName("success: when the owner creates a new project, the existing agent is included too")
    void enrollsAgentWhenOwnerCreatesProject() {
        // given
        Member owner = newHuman("owner");
        AgentResponse agent = agentService.createAgent(
                owner.getId(), CreateAgentCommand.builder().name("Bot").build());

        // when
        projectService.create(
                CreateProjectCommand.builder()
                        .projectKey("PROJ")
                        .title("Test Project")
                        .build(),
                owner.getId());

        // then
        assertThat(projectMemberQueryRepository.findAllWithProjectByMemberId(agent.id()))
                .extracting(member -> member.getProject().getKey())
                .containsExactly("PROJ");
    }

    @Test
    @DisplayName("success: when the owner is added to a project, their agents are included")
    void enrollsAgentsWhenOwnerAddedToProject() {
        // given
        Member manager = newHuman("manager");
        Member owner = newHuman("owner");
        AgentResponse agent = agentService.createAgent(
                owner.getId(), CreateAgentCommand.builder().name("Bot").build());
        Project project = newProject("PROJ");
        addAsManager(project, manager);

        // when
        projectMemberService.addMembers(ProjectIdentifier.ofProjectKey("PROJ"), Set.of(owner.getId()), manager.getId());

        // then
        assertThat(projectMemberQueryRepository.findByProjectAndMemberId(project, agent.id()))
                .isPresent();
    }

    @Test
    @DisplayName("success: when the owner leaves a project, their agents lose active membership")
    void revokesAgentsWhenOwnerLeaves() {
        // given
        Member owner = newHuman("owner");
        Project project = newProject("PROJ");
        addAsMember(project, owner);
        AgentResponse agent = agentService.createAgent(
                owner.getId(), CreateAgentCommand.builder().name("Bot").build());
        assertThat(projectMemberQueryRepository.findByProjectAndMemberId(project, agent.id()))
                .isPresent();

        // when
        projectMemberService.leave(ProjectIdentifier.ofProjectKey("PROJ"), owner.getId());

        // then
        assertThat(projectMemberQueryRepository.findByProjectAndMemberId(project, agent.id()))
                .isEmpty();
        assertThat(projectMemberQueryRepository.findByProjectAndMemberIdIncludingSoftDeleted(project, agent.id()))
                .isPresent();
    }

    @Test
    @DisplayName("success: re-including a revoked agent reactivates the same membership")
    void reEnrollReactivatesMembership() {
        // given
        Member owner = newHuman("owner");
        Project project = newProject("PROJ");
        addAsMember(project, owner);
        AgentResponse agent = agentService.createAgent(
                owner.getId(), CreateAgentCommand.builder().name("Bot").build());
        agentProjectJoinService.revokeAgentsOfMember(owner.getId(), project);
        assertThat(projectMemberQueryRepository.findByProjectAndMemberId(project, agent.id()))
                .isEmpty();

        // when
        agentProjectJoinService.includeAgentsOfMember(owner.getId(), project);

        // then
        assertThat(projectMemberQueryRepository.findByProjectAndMemberId(project, agent.id()))
                .isPresent();
    }
}
