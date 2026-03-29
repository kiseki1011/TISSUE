package com.tissue.feature.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.request.CreateProjectCommand;
import com.tissue.feature.project.application.dto.response.ProjectResponse;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.application.service.ProjectService;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.feature.project.domain.exception.DuplicateProjectKeyException;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberCommandRepository workspaceMemberCommandRepository;

    @Autowired
    private ProjectMemberQueryRepository projectMemberQueryRepository;

    private Member owner;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        owner = memberCommandRepository.save(Member.create("owner@tissue.com", "owner", "Jon Snow"));
        workspace = workspaceRepository.save(Workspace.create("WORKSPACE", "Test Workspace", null));
        workspaceMemberCommandRepository.save(WorkspaceMember.create(owner, workspace, WorkspaceRole.OWNER));
        em.flush();
    }

    @Nested
    @DisplayName("create project")
    class CreateProject {

        @Test
        @DisplayName("creating project also creates the creator as MANAGER")
        void creatorBecomesManager() {
            // given
            CreateProjectCommand cmd = new CreateProjectCommand("PROJ", "Test Project", null);

            // when
            ProjectResponse response = projectService.create("WORKSPACE", cmd, owner.getId());
            em.flush();
            em.clear();

            // then
            assertThat(response.projectKey()).isEqualTo("PROJ");

            assertThat(projectMemberQueryRepository.findWithWorkspaceMemberByKeysAndMemberId(
                            "WORKSPACE", "PROJ", owner.getId()))
                    .isPresent()
                    .get()
                    .satisfies(pm -> assertThat(pm.getRole()).isEqualTo(ProjectRole.MANAGER));
        }

        @Test
        @DisplayName("fails if project key already exists in the workspace")
        void failIfProjectKeyDuplicate() {
            // given
            CreateProjectCommand cmd = new CreateProjectCommand("PROJ", "Test Project", null);
            projectService.create("WORKSPACE", cmd, owner.getId());
            em.flush();

            CreateProjectCommand duplicateCmd = new CreateProjectCommand("PROJ", "Different Project", null);

            // when & then
            assertThatThrownBy(() -> projectService.create("WORKSPACE", duplicateCmd, owner.getId()))
                    .isInstanceOf(DuplicateProjectKeyException.class);
        }
    }
}
