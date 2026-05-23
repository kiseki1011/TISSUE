package com.tissue.feature.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberSearchResponse;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.application.service.WorkspaceMemberQueryService;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class WorkspaceMemberQueryServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private WorkspaceMemberQueryService workspaceMemberQueryService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberCommandRepository workspaceMemberCommandRepository;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private ProjectCommandRepository projectCommandRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberCommandRepository;

    @Test
    @DisplayName("can search members by name within a workspace")
    void searchMembers() {
        Workspace workspace = Workspace.create("WORKSPACE", "Test Workspace", null);
        workspaceRepository.save(workspace);

        Member member1 = Member.create("member1@test.com", "member1", "Gildong");
        Member member2 = Member.create("member2@test.com", "member2", "John");
        Member member3 = Member.create("member3@test.com", "member3", "Kaya");
        memberCommandRepository.save(member1);
        memberCommandRepository.save(member2);
        memberCommandRepository.save(member3);

        WorkspaceMember owner = WorkspaceMember.create(member1, workspace, WorkspaceRole.OWNER);
        WorkspaceMember wm2 = WorkspaceMember.create(member2, workspace, WorkspaceRole.MEMBER);
        WorkspaceMember wm3 = WorkspaceMember.create(member3, workspace, WorkspaceRole.MEMBER);
        workspaceMemberCommandRepository.save(owner);
        workspaceMemberCommandRepository.save(wm2);
        workspaceMemberCommandRepository.save(wm3);

        List<WorkspaceMemberSearchResponse> results =
                workspaceMemberQueryService.searchMembers("WORKSPACE", null, "Gil", member1.getId());

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().username()).isEqualTo("member1");
        assertThat(results.getFirst().displayName()).isEqualTo("Gildong");

        List<WorkspaceMemberSearchResponse> results2 =
                workspaceMemberQueryService.searchMembers("WORKSPACE", null, "mber2", member1.getId());
        assertThat(results2).hasSize(1);
        assertThat(results2.getFirst().username()).isEqualTo("member2");
        assertThat(results2.getFirst().displayName()).isEqualTo("John");
    }

    @Test
    @DisplayName("can filter by project key if provided")
    void searchProjectMembers() {
        Workspace workspace = Workspace.create("WORKSPACE", "Test Workspace", null);
        workspaceRepository.save(workspace);

        Project project = Project.create(workspace, "PROJ", "Test Project", null);
        projectCommandRepository.save(project);

        Member member1 = Member.create("member1@test.com", "member1", "Gildong");
        Member member2 = Member.create("member2@test.com", "member2", "John");
        Member member3 = Member.create("member3@test.com", "member3", "Kaya"); // not in project
        memberCommandRepository.save(member1);
        memberCommandRepository.save(member2);
        memberCommandRepository.save(member3);

        WorkspaceMember wm1 = WorkspaceMember.create(member1, workspace, WorkspaceRole.OWNER);
        WorkspaceMember wm2 = WorkspaceMember.create(member2, workspace, WorkspaceRole.MEMBER);
        WorkspaceMember wm3 = WorkspaceMember.create(member3, workspace, WorkspaceRole.MEMBER);
        workspaceMemberCommandRepository.save(wm1);
        workspaceMemberCommandRepository.save(wm2);
        workspaceMemberCommandRepository.save(wm3);

        ProjectMember pm1 = ProjectMember.create(project, wm1);
        ProjectMember pm2 = ProjectMember.create(project, wm2);
        projectMemberCommandRepository.save(pm1);
        projectMemberCommandRepository.save(pm2);

        List<WorkspaceMemberSearchResponse> results =
                workspaceMemberQueryService.searchMembers(workspace.getKey(), project.getKey(), "", member1.getId());

        // member3 should be excluded
        assertThat(results).hasSize(2);
        assertThat(results).extracting("username").containsExactlyInAnyOrder("member1", "member2");

        // search providedd with project key
        List<WorkspaceMemberSearchResponse> results2 = workspaceMemberQueryService.searchMembers(
                workspace.getKey(), project.getKey(), "Gildong", member1.getId());
        assertThat(results2).hasSize(1);
        assertThat(results2.getFirst().username()).isEqualTo("member1");
        assertThat(results2.getFirst().displayName()).isEqualTo("Gildong");
    }
}
