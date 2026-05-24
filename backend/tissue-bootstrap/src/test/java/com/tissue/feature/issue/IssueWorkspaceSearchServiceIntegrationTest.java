package com.tissue.feature.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.feature.issue.application.service.IssueLifecycleService;
import com.tissue.feature.issue.application.service.IssueParticipantService;
import com.tissue.feature.issue.application.service.IssueSearchService;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.exception.WorkspaceMemberNotFoundException;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class IssueWorkspaceSearchServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private IssueSearchService sut;

    @Autowired
    private IssueLifecycleService issueLifecycleService;

    @Autowired
    private IssueParticipantService issueParticipantService;

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
    private WorkflowRepository workflowRepository;

    @Autowired
    private IssueTypeRepository issueTypeRepository;

    private static final String WORKSPACE_KEY = "WORKSPACE";
    private static final ProjectIdentifier PROJECT_A = ProjectIdentifier.of(WORKSPACE_KEY, "PRA");
    private static final ProjectIdentifier PROJECT_B = ProjectIdentifier.of(WORKSPACE_KEY, "PRB");

    private Member gildong;
    private Member alice;
    private Member bob;
    private Long issueTypeIdA;
    private Long issueTypeIdB;

    @BeforeEach
    void setUp() {
        gildong = memberRepository.save(Member.create("gildong@tissue.com", "gildong", "Hong Gildong"));
        alice = memberRepository.save(Member.create("alice@tissue.com", "alice", "Alice"));
        bob = memberRepository.save(Member.create("bob@tissue.com", "bob", "Bob"));

        Workspace workspace = workspaceRepository.save(Workspace.create(WORKSPACE_KEY, "Workspace", null));

        WorkspaceMember gildongWorkspaceMember =
                workspaceMemberRepository.save(WorkspaceMember.create(gildong, workspace, WorkspaceRole.OWNER));
        WorkspaceMember aliceWorkspaceMember =
                workspaceMemberRepository.save(WorkspaceMember.create(alice, workspace, WorkspaceRole.MEMBER));
        // bob is a workspace member but not in any project — used to verify project-membership scoping
        workspaceMemberRepository.save(WorkspaceMember.create(bob, workspace, WorkspaceRole.MEMBER));

        Project projectA = projectRepository.save(Project.create(workspace, PROJECT_A.projectKey(), "Project A", null));
        Project projectB = projectRepository.save(Project.create(workspace, PROJECT_B.projectKey(), "Project B", null));

        // gildong is in both projects; alice is only in project A.
        projectMemberRepository.save(ProjectMember.createManager(projectA, gildongWorkspaceMember));
        projectMemberRepository.save(ProjectMember.createManager(projectB, gildongWorkspaceMember));
        projectMemberRepository.save(ProjectMember.create(projectA, aliceWorkspaceMember));

        issueTypeIdA = createIssueType(projectA);
        issueTypeIdB = createIssueType(projectB);

        em.flush();
        em.clear();

        setSecurityContext(gildong);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Long createIssueType(Project project) {
        Workflow workflow = Workflow.create(project, Name.of("Default-" + project.getKey()), null, ColorType.YELLOW);
        workflow.addState(Name.of("TODO"), null, ColorType.GREEN, StateCategory.INITIAL);
        workflow.addState(Name.of("DONE"), null, ColorType.BLACK, StateCategory.COMPLETED);
        workflowRepository.save(workflow);

        IssueType issueType = IssueType.create(
                project,
                Name.of("Story-" + project.getKey()),
                null,
                ColorType.RED,
                IconType.CIRCLE_FILLED,
                IssueHierarchy.STANDARD,
                workflow);
        issueTypeRepository.save(issueType);
        em.flush();
        return issueType.getId();
    }

    private void setSecurityContext(Member member) {
        MemberDetails details = new MemberDetails(member.getId(), member.getEmail(), member.getUsername(), List.of());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String createIssue(ProjectIdentifier pid, Long issueTypeId, String title, IssuePriority priority) {
        CreateIssueCommand cmd = CreateIssueCommand.builder()
                .title(title)
                .content("content")
                .summary("summary")
                .priority(priority)
                .dueAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .storyPoint(3)
                .issueTypeId(issueTypeId)
                .customFields(java.util.Map.of())
                .assigneeMemberId(gildong.getId())
                .build();
        var response = issueLifecycleService.create(pid, cmd, gildong.getId());
        em.flush();
        em.clear();
        return response.issueKey();
    }

    @Nested
    @DisplayName("searchByWorkspace")
    class SearchByWorkspace {

        @Test
        @DisplayName("returns issues from every project the actor belongs to")
        void returnsIssuesFromMemberProjects() {
            // given
            createIssue(PROJECT_A, issueTypeIdA, "Issue in A", IssuePriority.P2);
            createIssue(PROJECT_B, issueTypeIdB, "Issue in B", IssuePriority.P2);

            // when
            Page<IssueSummary> page = sut.searchByWorkspace(
                    WORKSPACE_KEY, IssueSearchCondition.empty(), PageRequest.of(0, 10), gildong.getId());

            // then
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent())
                    .extracting(IssueSummary::title)
                    .containsExactlyInAnyOrder("Issue in A", "Issue in B");
        }

        @Test
        @DisplayName("excludes issues from projects the actor is not a member of")
        void excludesIssuesFromNonMemberProjects() {
            // given
            createIssue(PROJECT_A, issueTypeIdA, "Issue in A", IssuePriority.P2);
            createIssue(PROJECT_B, issueTypeIdB, "Issue in B", IssuePriority.P2);

            // when — alice is only in project A
            Page<IssueSummary> page = sut.searchByWorkspace(
                    WORKSPACE_KEY, IssueSearchCondition.empty(), PageRequest.of(0, 10), alice.getId());

            // then
            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent().getFirst().title()).isEqualTo("Issue in A");
        }

        @Test
        @DisplayName("returns empty page for a workspace member who joined no projects")
        void returnsEmptyForBareWorkspaceMember() {
            // given
            createIssue(PROJECT_A, issueTypeIdA, "Issue in A", IssuePriority.P2);
            createIssue(PROJECT_B, issueTypeIdB, "Issue in B", IssuePriority.P2);

            // when — bob has no project membership
            Page<IssueSummary> page = sut.searchByWorkspace(
                    WORKSPACE_KEY, IssueSearchCondition.empty(), PageRequest.of(0, 10), bob.getId());

            // then
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("rejects a non-workspace-member")
        void rejectsNonWorkspaceMember() {
            // given
            Member outsider = memberRepository.save(Member.create("outsider@tissue.com", "outsider", "Outsider"));
            em.flush();

            // when & then
            assertThatThrownBy(() -> sut.searchByWorkspace(
                            WORKSPACE_KEY, IssueSearchCondition.empty(), PageRequest.of(0, 10), outsider.getId()))
                    .isInstanceOf(WorkspaceMemberNotFoundException.class);
        }

        @Test
        @DisplayName("filters by subscriberMemberIds across the workspace")
        void filtersBySubscriber() {
            // given
            String issueInA = createIssue(PROJECT_A, issueTypeIdA, "Subscribed Issue in A", IssuePriority.P2);
            createIssue(PROJECT_A, issueTypeIdA, "Not subscribed", IssuePriority.P2);
            createIssue(PROJECT_B, issueTypeIdB, "In B", IssuePriority.P2); // alice not a member of B

            // alice subscribes to one issue in project A
            issueParticipantService.subscribe(IssueIdentifier.of(WORKSPACE_KEY, issueInA), alice.getId());
            em.flush();
            em.clear();

            IssueSearchCondition condition = new IssueSearchCondition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Set.of(alice.getId()),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);

            // when — actor=gildong (sees project A, B), filter subscriberMemberIds=[alice,]
            Page<IssueSummary> page =
                    sut.searchByWorkspace(WORKSPACE_KEY, condition, PageRequest.of(0, 10), gildong.getId());

            // then
            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent().getFirst().title()).isEqualTo("Subscribed Issue in A");
        }

        @Test
        @DisplayName("filters by reviewerMemberIds across the workspace")
        void filtersByReviewer() {
            // given
            String issueInA = createIssue(PROJECT_A, issueTypeIdA, "Reviewed in A", IssuePriority.P2);
            createIssue(PROJECT_B, issueTypeIdB, "Not reviewed", IssuePriority.P2);

            // gildong adds alice as a reviewer on an issue in project A
            issueParticipantService.addReviewer(
                    IssueIdentifier.of(WORKSPACE_KEY, issueInA), alice.getId(), gildong.getId());
            em.flush();
            em.clear();

            IssueSearchCondition condition = new IssueSearchCondition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    Set.of(alice.getId()),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);

            // when
            Page<IssueSummary> page =
                    sut.searchByWorkspace(WORKSPACE_KEY, condition, PageRequest.of(0, 10), gildong.getId());

            // then
            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent().getFirst().title()).isEqualTo("Reviewed in A");
        }
    }
}
