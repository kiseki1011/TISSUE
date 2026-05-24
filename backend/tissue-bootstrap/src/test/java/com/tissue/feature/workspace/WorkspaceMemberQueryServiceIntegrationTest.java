package com.tissue.feature.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberDetail;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberSearchResponse;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberSummary;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.application.service.WorkspaceMemberQueryService;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.exception.WorkspaceMemberNotFoundException;
import com.tissue.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class WorkspaceMemberQueryServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private WorkspaceMemberQueryService sut;

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

    @Nested
    @DisplayName("searchMembers")
    class SearchMembers {

        @Test
        @DisplayName("can search members by name within a workspace")
        void searchMembersByName() {
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

            List<WorkspaceMemberSearchResponse> results = sut.searchMembers("WORKSPACE", null, "Gil", member1.getId());

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().username()).isEqualTo("member1");
            assertThat(results.getFirst().displayName()).isEqualTo("Gildong");

            List<WorkspaceMemberSearchResponse> results2 =
                    sut.searchMembers("WORKSPACE", null, "mber2", member1.getId());

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
                    sut.searchMembers(workspace.getKey(), project.getKey(), "", member1.getId());

            // member3 should be excluded
            assertThat(results).hasSize(2);
            assertThat(results).extracting("username").containsExactlyInAnyOrder("member1", "member2");

            // search providedd with project key
            List<WorkspaceMemberSearchResponse> results2 =
                    sut.searchMembers(workspace.getKey(), project.getKey(), "Gildong", member1.getId());
            assertThat(results2).hasSize(1);
            assertThat(results2.getFirst().username()).isEqualTo("member1");
            assertThat(results2.getFirst().displayName()).isEqualTo("Gildong");
        }
    }

    @Nested
    @DisplayName("getWorkspaceMembers")
    class GetWorkspaceMembers {

        private Member gildong;
        private Member alice;
        private Member bob;
        private Workspace workspace;

        @BeforeEach
        void setUp() {
            gildong = memberCommandRepository.save(Member.create("gildong@tissue.com", "gildong", "Hong Gildong"));
            alice = memberCommandRepository.save(Member.create("alice@tissue.com", "alice", "Alice"));
            bob = memberCommandRepository.save(Member.create("bob@tissue.com", "bob", "Bob"));

            workspace = workspaceRepository.save(Workspace.create("WORKSPACE", "Workspace", null));

            workspaceMemberCommandRepository.save(WorkspaceMember.create(gildong, workspace, WorkspaceRole.OWNER));
            workspaceMemberCommandRepository.save(WorkspaceMember.create(alice, workspace, WorkspaceRole.ADMIN));
            workspaceMemberCommandRepository.save(WorkspaceMember.create(bob, workspace, WorkspaceRole.MEMBER));

            em.flush();
            em.clear();
        }

        @Test
        @DisplayName("returns every active member of the workspace when no keyword is provided")
        void returnsAllActiveMembers() {
            // when
            Page<WorkspaceMemberSummary> page =
                    sut.getWorkspaceMembers("WORKSPACE", null, PageRequest.of(0, 10), gildong.getId());

            // then
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getContent())
                    .extracting(WorkspaceMemberSummary::username)
                    .containsExactlyInAnyOrder("gildong", "alice", "bob");
        }

        @Test
        @DisplayName("filters by name or username when keyword is provided")
        void filtersByKeyword() {
            // when
            Page<WorkspaceMemberSummary> byName =
                    sut.getWorkspaceMembers("WORKSPACE", "Gil", PageRequest.of(0, 10), gildong.getId());
            Page<WorkspaceMemberSummary> byUsername =
                    sut.getWorkspaceMembers("WORKSPACE", "ali", PageRequest.of(0, 10), gildong.getId());

            // then
            assertThat(byName.getTotalElements()).isEqualTo(1);
            assertThat(byName.getContent().getFirst().username()).isEqualTo("gildong");
            assertThat(byUsername.getTotalElements()).isEqualTo(1);
            assertThat(byUsername.getContent().getFirst().username()).isEqualTo("alice");
        }

        @Test
        @DisplayName("treats a blank keyword the same as no keyword")
        void treatsBlankKeywordAsNoFilter() {
            // when
            Page<WorkspaceMemberSummary> page =
                    sut.getWorkspaceMembers("WORKSPACE", "   ", PageRequest.of(0, 10), gildong.getId());

            // then
            assertThat(page.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("rejects if actor is non workspace member")
        void rejectsNonWorkspaceMember() {
            // given
            Member outsider =
                    memberCommandRepository.save(Member.create("outsider@tissue.com", "outsider", "Outsider"));

            // when & then
            assertThatThrownBy(
                            () -> sut.getWorkspaceMembers("WORKSPACE", null, PageRequest.of(0, 10), outsider.getId()))
                    .isInstanceOf(WorkspaceMemberNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getWorkspaceMemberDetail")
    class GetWorkspaceMemberDetail {

        private Member gildong;
        private Member alice;
        private Workspace workspace;

        @BeforeEach
        void setUp() {
            gildong = memberCommandRepository.save(Member.create("gildong@tissue.com", "gildong", "Hong Gildong"));
            alice = memberCommandRepository.save(Member.create("alice@tissue.com", "alice", "Alice"));

            workspace = workspaceRepository.save(Workspace.create("WORKSPACE", "Workspace", null));

            workspaceMemberCommandRepository.save(WorkspaceMember.create(gildong, workspace, WorkspaceRole.OWNER));
            workspaceMemberCommandRepository.save(WorkspaceMember.create(alice, workspace, WorkspaceRole.ADMIN));

            em.flush();
            em.clear();
        }

        @Test
        @DisplayName("returns the member's profile fields")
        void returnsMemberProfile() {
            // when
            WorkspaceMemberDetail detail = sut.getWorkspaceMemberDetail("WORKSPACE", alice.getId(), gildong.getId());

            // then
            assertThat(detail.workspaceKey()).isEqualTo("WORKSPACE");
            assertThat(detail.memberId()).isEqualTo(alice.getId());
            assertThat(detail.username()).isEqualTo("alice");
            assertThat(detail.displayName()).isEqualTo("Alice");
            assertThat(detail.email()).isEqualTo("alice@tissue.com");
            assertThat(detail.role()).isEqualTo(WorkspaceRole.ADMIN);
            assertThat(detail.joinedAt()).isNotNull();
        }

        @Test
        @DisplayName("rejects if actor is non workspace member")
        void rejectsNonWorkspaceMemberActor() {
            // given
            Member outsider =
                    memberCommandRepository.save(Member.create("outsider@tissue.com", "outsider", "Outsider"));

            // when & then
            assertThatThrownBy(() -> sut.getWorkspaceMemberDetail("WORKSPACE", alice.getId(), outsider.getId()))
                    .isInstanceOf(WorkspaceMemberNotFoundException.class);
        }

        @Test
        @DisplayName("throws when the target member is not part of the workspace")
        void throwsWhenTargetNotFound() {
            // when & then
            assertThatThrownBy(() -> sut.getWorkspaceMemberDetail("WORKSPACE", 999L, gildong.getId()))
                    .isInstanceOf(WorkspaceMemberNotFoundException.class);
        }
    }
}
