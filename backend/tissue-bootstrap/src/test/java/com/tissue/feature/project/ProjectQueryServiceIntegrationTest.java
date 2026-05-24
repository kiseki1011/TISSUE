package com.tissue.feature.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.response.ProjectDetail;
import com.tissue.feature.project.application.dto.response.ProjectSummary;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectQueryRepository;
import com.tissue.feature.project.application.service.ProjectQueryService;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectVisibility;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.exception.WorkspaceMemberNotFoundException;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectQueryServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProjectQueryService sut;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberCommandRepository workspaceMemberRepository;

    @Autowired
    private ProjectCommandRepository projectRepository;

    @Autowired
    private ProjectQueryRepository projectQueryRepository;

    private static final String WORKSPACE_KEY = "WORKSPACE";

    private Member gildong;
    private Member bob;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        gildong = memberRepository.save(Member.create("gildong@tissue.com", "gildong", "Hong Gildong"));
        bob = memberRepository.save(Member.create("bob@tissue.com", "bob", "Bob"));
        workspace = workspaceRepository.save(Workspace.create(WORKSPACE_KEY, "Workspace", null));
        workspaceMemberRepository.save(WorkspaceMember.create(gildong, workspace, WorkspaceRole.OWNER));
        em.flush();
        em.clear();
    }

    private void createProject(String key, String title) {
        Workspace managedWorkspace =
                workspaceRepository.findByKey(workspace.getKey()).orElseThrow();
        projectRepository.save(Project.create(managedWorkspace, key, title, null));
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("getProjects")
    class GetProjects {

        @Test
        @DisplayName("returns all non archived projects in the workspace")
        void returnsAllNonArchivedProjects() {
            // given
            createProject("ALPHA", "Alpha");
            createProject("BETA", "Beta");

            // when
            Page<ProjectSummary> page =
                    sut.getProjects(WORKSPACE_KEY, false, null, PageRequest.of(0, 10), gildong.getId());

            // then
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent()).extracting(ProjectSummary::key).containsExactlyInAnyOrder("ALPHA", "BETA");
            assertThat(page.getContent()).allMatch(p -> p.visibility() == ProjectVisibility.PRIVATE);
        }

        @Test
        @DisplayName("matches the keyword against title and key case-insensitive")
        void matchesKeywordAgainstTitleAndKey() {
            // given
            createProject("AUTH", "Authentication");
            createProject("BILL", "Billing");

            // when
            Page<ProjectSummary> page =
                    sut.getProjects(WORKSPACE_KEY, false, "auth", PageRequest.of(0, 10), gildong.getId());

            // then
            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent().getFirst().key()).isEqualTo("AUTH");
        }

        @Test
        @DisplayName("excludes archived projects by default and includes them when requested")
        void excludesArchivedByDefault() {
            // given
            createProject("ALPHA", "Alpha");
            createProject("BETA", "Beta");
            Project beta = projectQueryRepository
                    .findByWorkspaceKeyAndKey(WORKSPACE_KEY, "BETA")
                    .orElseThrow();
            beta.archive();
            em.flush();
            em.clear();

            // when
            Page<ProjectSummary> excludingArchived =
                    sut.getProjects(WORKSPACE_KEY, false, null, PageRequest.of(0, 10), gildong.getId());
            Page<ProjectSummary> includingArchived =
                    sut.getProjects(WORKSPACE_KEY, true, null, PageRequest.of(0, 10), gildong.getId());

            // then
            assertThat(excludingArchived.getContent())
                    .extracting(ProjectSummary::key)
                    .containsExactly("ALPHA");
            assertThat(includingArchived.getContent())
                    .extracting(ProjectSummary::key)
                    .containsExactlyInAnyOrder("ALPHA", "BETA");
        }

        @Test
        @DisplayName("returns one element per page when size is 1 and the total count")
        void paginates() {
            // given
            createProject("ALPHA", "Alpha");
            createProject("BETA", "Beta");
            createProject("GAMMA", "Gamma");

            // when
            Page<ProjectSummary> page = sut.getProjects(
                    WORKSPACE_KEY,
                    false,
                    null,
                    PageRequest.of(0, 1, Sort.by("key").ascending()),
                    gildong.getId());

            // then
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().getFirst().key()).isEqualTo("ALPHA");
        }

        @Test
        @DisplayName("rejects if actor is non workspace member before running query")
        void rejectsNonWorkspaceMember() {
            // given
            createProject("ALPHA", "Alpha");

            // when & then
            assertThatThrownBy(() -> sut.getProjects(WORKSPACE_KEY, false, null, PageRequest.of(0, 10), bob.getId()))
                    .isInstanceOf(WorkspaceMemberNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getProjectDetail")
    class GetProjectDetail {

        @Test
        @DisplayName("returns project metadata for a workspace member")
        void returnsMetadata() {
            // given
            createProject("ALPHA", "Alpha");

            // when
            ProjectDetail detail = sut.getProjectDetail(ProjectIdentifier.of(WORKSPACE_KEY, "ALPHA"), gildong.getId());

            // then
            assertThat(detail.key()).isEqualTo("ALPHA");
            assertThat(detail.title()).isEqualTo("Alpha");
            assertThat(detail.visibility()).isEqualTo(ProjectVisibility.PRIVATE);
            assertThat(detail.archived()).isFalse();
        }
    }
}
