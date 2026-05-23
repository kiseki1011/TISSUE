package com.tissue.feature.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.response.ProjectMemberSummary;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.service.ProjectMemberQueryService;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.feature.project.domain.exception.ProjectMemberNotFoundException;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectMemberQueryServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProjectMemberQueryService sut;

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

    private static final ProjectIdentifier PROJECT_IDENTIFIER = ProjectIdentifier.of("WORKSPACE", "PROJ");

    private Member gildong;
    private Member alice;
    private Member bob;

    @BeforeEach
    void setUp() {
        gildong = memberRepository.save(Member.create("gildong@tissue.com", "gildong", "Hong Gildong"));
        alice = memberRepository.save(Member.create("alice@tissue.com", "alice", "Alice"));
        bob = memberRepository.save(Member.create("bob@tissue.com", "bob", "Bob"));

        Workspace workspace =
                workspaceRepository.save(Workspace.create(PROJECT_IDENTIFIER.workspaceKey(), "Workspace", null));
        Project project =
                projectRepository.save(Project.create(workspace, PROJECT_IDENTIFIER.projectKey(), "Project", null));

        WorkspaceMember gildongWorkspaceMember =
                workspaceMemberRepository.save(WorkspaceMember.create(gildong, workspace, WorkspaceRole.OWNER));
        WorkspaceMember aliceWorkspaceMember =
                workspaceMemberRepository.save(WorkspaceMember.create(alice, workspace, WorkspaceRole.MEMBER));

        projectMemberRepository.save(ProjectMember.createManager(project, gildongWorkspaceMember));
        projectMemberRepository.save(ProjectMember.create(project, aliceWorkspaceMember));

        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("getProjectMembers")
    class GetProjectMembers {

        @Test
        @DisplayName("returns every active member of the project")
        void returnsEveryActiveMember() {
            // when
            Page<ProjectMemberSummary> page =
                    sut.getProjectMembers(PROJECT_IDENTIFIER, null, null, PageRequest.of(0, 10), gildong.getId());

            // then
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent())
                    .extracting(ProjectMemberSummary::username)
                    .containsExactlyInAnyOrder("gildong", "alice");
        }

        @Test
        @DisplayName("filters by role MANAGER")
        void filtersByRole() {
            // when
            Page<ProjectMemberSummary> page = sut.getProjectMembers(
                    PROJECT_IDENTIFIER, ProjectRole.MANAGER, null, PageRequest.of(0, 10), gildong.getId());

            // then
            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent().getFirst().username()).isEqualTo("gildong");
            assertThat(page.getContent().getFirst().role()).isEqualTo(ProjectRole.MANAGER);
        }

        @Test
        @DisplayName("matches the keyword against display name case-insensitive")
        void matchesKeywordAgainstDisplayName() {
            // when
            Page<ProjectMemberSummary> page =
                    sut.getProjectMembers(PROJECT_IDENTIFIER, null, "ALIC", PageRequest.of(0, 10), gildong.getId());

            // then
            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent().getFirst().username()).isEqualTo("alice");
        }

        @Test
        @DisplayName("rejects non project member")
        void rejectsNonProjectMember() {
            // when / then
            assertThatThrownBy(() ->
                            sut.getProjectMembers(PROJECT_IDENTIFIER, null, null, PageRequest.of(0, 10), bob.getId()))
                    .isInstanceOf(ProjectMemberNotFoundException.class);
        }
    }
}
