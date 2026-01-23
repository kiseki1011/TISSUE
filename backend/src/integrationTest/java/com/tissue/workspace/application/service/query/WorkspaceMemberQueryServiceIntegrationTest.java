package com.tissue.workspace.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.member.application.port.out.MemberCommandRepository;
import com.tissue.member.domain.Member;
import com.tissue.project.application.port.out.ProjectCommandRepository;
import com.tissue.project.application.port.out.ProjectMemberCommandRepository;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.support.IntegrationTestSupport;
import com.tissue.workspace.adapter.in.web.dto.response.WorkspaceMemberSearchResponse;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.port.out.WorkspaceCommandRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberCommandRepository;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class WorkspaceMemberQueryServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private WorkspaceMemberQueryService sut;

    @Autowired
    private WorkspaceCommandRepository workspaceCommandRepository;

    @Autowired
    private WorkspaceMemberCommandRepository workspaceMemberCommandRepository;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private ProjectCommandRepository projectCommandRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberCommandRepository;

    @Test
    @DisplayName("searchMembers: searches members by name within a workspace")
    void searchMembers() {
        Workspace workspace = Workspace.create("TEST-WS", "Test Workspace", "Description");
        workspaceCommandRepository.save(workspace);

        Member member1 = Member.create("member1@test.com", "member1", "Gildong");
        Member member2 = Member.create("member2@test.com", "member2", "Chulsoo");
        Member member3 = Member.create("member3@test.com", "member3", "Younghee");
        memberCommandRepository.save(member1);
        memberCommandRepository.save(member2);
        memberCommandRepository.save(member3);

        WorkspaceMember owner = WorkspaceMember.create(member1, workspace, WorkspaceRole.OWNER);
        WorkspaceMember wm2 = WorkspaceMember.create(member2, workspace, WorkspaceRole.MEMBER);
        WorkspaceMember wm3 = WorkspaceMember.create(member3, workspace, WorkspaceRole.MEMBER);
        workspaceMemberCommandRepository.save(owner);
        workspaceMemberCommandRepository.save(wm2);
        workspaceMemberCommandRepository.save(wm3);

        WorkspaceMemberContext actorContext = new WorkspaceMemberContext(
                workspace.getId(),
                member1.getId(),
                owner.getId(),
                workspace.getKey(),
                member1.getEmail(),
                member1.getName(),
                WorkspaceRole.OWNER);

        List<WorkspaceMemberSearchResponse> results = sut.searchMembers(actorContext, "Gil", null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).username()).isEqualTo("member1");
        assertThat(results.get(0).displayName()).isEqualTo("Gildong");

        List<WorkspaceMemberSearchResponse> results2 = sut.searchMembers(actorContext, "mber2", null);
        assertThat(results2).hasSize(1);
        assertThat(results2.get(0).username()).isEqualTo("member2");
        assertThat(results2.get(0).displayName()).isEqualTo("Chulsoo");
    }

    @Test
    @DisplayName("searchMembers: filters by project key if provided")
    void searchProjectMembers() {
        Workspace workspace = Workspace.create("TEST-WS", "Test Workspace", "Description");
        workspaceCommandRepository.save(workspace);

        Project project = Project.create(workspace, "PROJ", "Test Project", "Desc");
        projectCommandRepository.save(project);

        Member member1 = Member.create("member1@test.com", "member1", "Gildong");
        Member member2 = Member.create("member2@test.com", "member2", "Chulsoo");
        Member member3 = Member.create("member3@test.com", "member3", "Younghee"); // not in project
        memberCommandRepository.save(member1);
        memberCommandRepository.save(member2);
        memberCommandRepository.save(member3);

        WorkspaceMember wm1 = WorkspaceMember.create(member1, workspace, WorkspaceRole.OWNER);
        WorkspaceMember wm2 = WorkspaceMember.create(member2, workspace, WorkspaceRole.MEMBER);
        WorkspaceMember wm3 = WorkspaceMember.create(member3, workspace, WorkspaceRole.MEMBER);
        workspaceMemberCommandRepository.save(wm1);
        workspaceMemberCommandRepository.save(wm2);
        workspaceMemberCommandRepository.save(wm3);

        ProjectMember pm1 = ProjectMember.create(project, wm1, ProjectRole.ADMIN);
        ProjectMember pm2 = ProjectMember.create(project, wm2, ProjectRole.VIEWER);
        projectMemberCommandRepository.save(pm1);
        projectMemberCommandRepository.save(pm2);

        WorkspaceMemberContext context = new WorkspaceMemberContext(
                workspace.getId(),
                member1.getId(),
                wm1.getId(),
                workspace.getKey(),
                member1.getEmail(),
                member1.getName(),
                WorkspaceRole.OWNER);

        List<WorkspaceMemberSearchResponse> results = sut.searchMembers(context, "", "PROJ");

        // member3 should be excluded
        assertThat(results).hasSize(2);
        assertThat(results).extracting("username").containsExactlyInAnyOrder("member1", "member2");

        // search with query + project key
        List<WorkspaceMemberSearchResponse> results2 = sut.searchMembers(context, "Gildong", "PROJ");
        assertThat(results2).hasSize(1);
        assertThat(results2.get(0).username()).isEqualTo("member1");
        assertThat(results2.get(0).displayName()).isEqualTo("Gildong");
    }
}
