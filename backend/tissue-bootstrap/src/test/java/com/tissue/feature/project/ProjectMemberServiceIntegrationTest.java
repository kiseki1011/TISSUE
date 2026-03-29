package com.tissue.feature.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.response.ProjectMembersResponse;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.application.service.ProjectMemberService;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
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
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectMemberServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProjectMemberService projectMemberService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberCommandRepository workspaceMemberCommandRepository;

    @Autowired
    private ProjectCommandRepository projectCommandRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberCommandRepository;

    @Autowired
    private ProjectMemberQueryRepository projectMemberQueryRepository;

    private Member manager;
    private Workspace workspace;
    private Project project;

    @BeforeEach
    void setUp() {
        manager = memberCommandRepository.save(Member.create("manager@tissue.com", "manager", "John Wick"));
        workspace = workspaceRepository.save(Workspace.create("WORKSPACE", "Test Workspace", null));
        WorkspaceMember managerWm =
                workspaceMemberCommandRepository.save(WorkspaceMember.create(manager, workspace, WorkspaceRole.OWNER));

        project = Project.create(workspace, "PROJ", "Test Project", null);
        projectCommandRepository.save(project);
        projectMemberCommandRepository.save(ProjectMember.createManager(project, managerWm));
        em.flush();
    }

    @Nested
    @DisplayName("add members")
    class AddMembers {

        @Test
        @DisplayName("adds only new members and skips already existing members")
        void addsNewAndSkipsExisting() {
            // given
            Member newMember = memberCommandRepository.save(Member.create("new@tissue.com", "newuser", "HongGilDong"));
            workspaceMemberCommandRepository.save(WorkspaceMember.create(newMember, workspace, WorkspaceRole.MEMBER));

            Member existingMember =
                    memberCommandRepository.save(Member.create("existing@tissue.com", "existing", "KimChulSoo"));
            WorkspaceMember existingWm = workspaceMemberCommandRepository.save(
                    WorkspaceMember.create(existingMember, workspace, WorkspaceRole.MEMBER));
            projectMemberCommandRepository.save(ProjectMember.create(project, existingWm));
            em.flush();

            ProjectIdentifier pid = ProjectIdentifier.of("WORKSPACE", "PROJ");

            // when
            ProjectMembersResponse response = projectMemberService.addMembers(
                    pid, Set.of(newMember.getId(), existingMember.getId()), manager.getId());
            em.flush();
            em.clear();

            // then
            assertThat(response.memberIds()).containsExactly(newMember.getId());
            assertThat(response.totalSize()).isEqualTo(1);

            assertThat(projectMemberQueryRepository.findWithWorkspaceMemberByKeysAndMemberId(
                            "WORKSPACE", "PROJ", newMember.getId()))
                    .isPresent();
        }
    }
}
