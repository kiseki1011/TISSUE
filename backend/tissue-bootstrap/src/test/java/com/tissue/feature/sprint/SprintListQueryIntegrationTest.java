package com.tissue.feature.sprint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.exception.ProjectMemberNotFoundException;
import com.tissue.feature.sprint.application.dto.response.SprintSummary;
import com.tissue.feature.sprint.application.port.repository.SprintCommandRepository;
import com.tissue.feature.sprint.application.service.SprintQueryService;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.sprint.domain.SprintStatus;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.support.IntegrationTestSupport;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class SprintListQueryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private SprintQueryService sut;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberCommandRepository workspaceMemberRepository;

    @Autowired
    private ProjectCommandRepository projectRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberRepository;

    @Autowired
    private SprintCommandRepository sprintRepository;

    private static final ProjectIdentifier PROJECT_IDENTIFIER = ProjectIdentifier.of("WORKSPACE", "PROJ");

    private Member gildong;
    private Member bob;
    private Project project;

    @BeforeEach
    void setUp() {
        gildong = memberRepository.save(Member.create("gildong@tissue.com", "gildong", "Hong Gildong"));
        bob = memberRepository.save(Member.create("bob@tissue.com", "bob", "Bob"));

        Workspace workspace =
                workspaceRepository.save(Workspace.create(PROJECT_IDENTIFIER.workspaceKey(), "Workspace", null));
        project = projectRepository.save(Project.create(workspace, PROJECT_IDENTIFIER.projectKey(), "Project", null));

        WorkspaceMember gildongWorkspaceMember =
                workspaceMemberRepository.save(WorkspaceMember.create(gildong, workspace, WorkspaceRole.OWNER));
        projectMemberRepository.save(ProjectMember.createManager(project, gildongWorkspaceMember));

        em.flush();
        em.clear();
    }

    private void createSprint(String title) {
        Project managedProject = em.find(Project.class, project.getId());
        sprintRepository.save(Sprint.create(managedProject, title, null));
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("getProjectSprints")
    class GetProjectSprints {

        @Test
        @DisplayName("returns every sprint of the project when no status filter is supplied")
        void returnsAllSprintsWhenNoStatusFilter() {
            // given
            createSprint("Sprint 1");
            createSprint("Sprint 2");

            // when
            Page<SprintSummary> page =
                    sut.getProjectSprints(PROJECT_IDENTIFIER, null, PageRequest.of(0, 10), gildong.getId());

            // then
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent()).allMatch(s -> s.status() == SprintStatus.PLANNING);
        }

        @Test
        @DisplayName("treats an empty status set the same as no filter")
        void treatsEmptyStatusSetAsNoFilter() {
            // given
            createSprint("Sprint 1");

            // when
            Page<SprintSummary> page =
                    sut.getProjectSprints(PROJECT_IDENTIFIER, Set.of(), PageRequest.of(0, 10), gildong.getId());

            // then
            assertThat(page.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("filters by the given status set")
        void filtersBySupplied() {
            // given — new sprints are in PLANNING
            createSprint("Sprint 1");

            // when
            Page<SprintSummary> page = sut.getProjectSprints(
                    PROJECT_IDENTIFIER, Set.of(SprintStatus.ACTIVE), PageRequest.of(0, 10), gildong.getId());

            // then
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("rejects a non-project-member")
        void rejectsNonProjectMember() {
            // when & then
            assertThatThrownBy(
                            () -> sut.getProjectSprints(PROJECT_IDENTIFIER, null, PageRequest.of(0, 10), bob.getId()))
                    .isInstanceOf(ProjectMemberNotFoundException.class);
        }
    }
}
