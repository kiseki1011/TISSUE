package com.tissue.feature.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.feature.issue.application.service.IssueLifecycleService;
import com.tissue.feature.issue.application.service.IssueSearchService;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.exception.ProjectMemberNotFoundException;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class IssueSearchServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private IssueSearchService sut;

    @Autowired
    private IssueLifecycleService issueLifecycleService;

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

    private static final ProjectIdentifier PID = new ProjectIdentifier("WSP", "PROJ");

    private Member actor;
    private Member outsider;
    private Long issueTypeId;
    private Long fieldId;

    @BeforeEach
    void setUp() {
        actor = memberRepository.save(Member.create("actor@tissue.com", "actor", "Actor"));
        outsider = memberRepository.save(Member.create("outsider@tissue.com", "outsider", "Outsider"));

        Workspace workspace = workspaceRepository.save(Workspace.create(PID.workspaceKey(), "WS", null));
        Project project = projectRepository.save(Project.create(workspace, PID.projectKey(), "Proj", null));
        WorkspaceMember actorWm =
                workspaceMemberRepository.save(WorkspaceMember.create(actor, workspace, WorkspaceRole.OWNER));
        projectMemberRepository.save(ProjectMember.createManager(project, actorWm));

        Workflow workflow = Workflow.create(project, Name.of("Default"), null, ColorType.YELLOW);
        WorkflowState todo = workflow.addState(Name.of("TODO"), null, ColorType.GREEN, StateCategory.INITIAL);
        WorkflowState inProgress =
                workflow.addState(Name.of("IN PROGRESS"), null, ColorType.BLUE, StateCategory.ACTIVE);
        WorkflowState done = workflow.addState(Name.of("DONE"), null, ColorType.BLACK, StateCategory.COMPLETED);
        workflow.addTransition(Name.of("Start"), null, todo, inProgress);
        workflow.addTransition(Name.of("Complete"), null, inProgress, done);
        workflowRepository.save(workflow);

        IssueType issueType = IssueType.create(
                project,
                Name.of("Story"),
                null,
                ColorType.RED,
                IconType.CIRCLE_FILLED,
                IssueHierarchy.STANDARD,
                workflow);
        issueTypeRepository.save(issueType);
        IssueField goalField = issueType.addField(Name.of("goal"), "Goal", IssueFieldType.TEXT, true, 0);

        em.flush();
        issueTypeId = issueType.getId();
        fieldId = goalField.getId();
        em.clear();

        setSecurityContext(actor);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(Member member) {
        MemberDetails details = new MemberDetails(member.getId(), member.getEmail(), member.getUsername(), List.of());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String createIssue(String title, IssuePriority priority, Integer storyPoint) {
        CreateIssueCommand cmd = CreateIssueCommand.builder()
                .sprintId(null)
                .parentProjectKey(null)
                .parentKey(null)
                .title(title)
                .content("content")
                .summary("summary")
                .priority(priority)
                .dueAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .storyPoint(storyPoint)
                .issueTypeId(issueTypeId)
                .customFields(Map.of(fieldId, "fieldValue"))
                .assigneeMemberId(actor.getId())
                .build();
        var response = issueLifecycleService.create(PID, cmd, actor.getId());
        em.flush();
        em.clear();
        return response.issueKey();
    }

    @Test
    @DisplayName("returns all issues in the project when no filter is given (default sort)")
    void noFilter_returnsAll() {
        // given
        createIssue("first", IssuePriority.P2, 3);
        createIssue("second", IssuePriority.P0, 5);

        // when
        Page<IssueSummary> page =
                sut.searchByProject(PID, IssueSearchCondition.empty(), PageRequest.of(0, 10), actor.getId());

        // then — default sort: priority asc
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).priority()).isEqualTo(IssuePriority.P0);
        assertThat(page.getContent().get(1).priority()).isEqualTo(IssuePriority.P2);
    }

    @Test
    @DisplayName("filters by priority set")
    void filterByPriority() {
        // given
        createIssue("p0-ticket", IssuePriority.P0, 1);
        createIssue("p2-ticket", IssuePriority.P2, 1);
        createIssue("p3-ticket", IssuePriority.P3, 1);

        IssueSearchCondition cond = new IssueSearchCondition(
                Set.of(IssuePriority.P0, IssuePriority.P2),
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
                null,
                null,
                null,
                null,
                null,
                null);

        // when
        Page<IssueSummary> page = sut.searchByProject(PID, cond, PageRequest.of(0, 10), actor.getId());

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(IssueSummary::priority)
                .containsExactlyInAnyOrder(IssuePriority.P0, IssuePriority.P2);
    }

    @Test
    @DisplayName("filters by state category INITIAL")
    void filterByStateCategory() {
        // given
        createIssue("a", IssuePriority.P2, 1);
        createIssue("b", IssuePriority.P3, 1);

        IssueSearchCondition cond = new IssueSearchCondition(
                null,
                Set.of(StateCategory.INITIAL),
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
                null,
                null,
                null,
                null,
                null);

        // when
        Page<IssueSummary> page = sut.searchByProject(PID, cond, PageRequest.of(0, 10), actor.getId());

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(s -> s.currentStateCategory() == StateCategory.INITIAL);
    }

    @Test
    @DisplayName("keyword matches issue title (case-insensitive)")
    void keywordMatchesTitle() {
        // given
        createIssue("Add login screen", IssuePriority.P2, 1);
        createIssue("Refactor billing", IssuePriority.P2, 1);

        IssueSearchCondition cond = new IssueSearchCondition(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                "LOGIN");

        // when
        Page<IssueSummary> page = sut.searchByProject(PID, cond, PageRequest.of(0, 10), actor.getId());

        // then
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).title()).isEqualTo("Add login screen");
    }

    @Test
    @DisplayName("pagination with pageSize=1 returns first page with one entry and totalElements=3")
    void pagination_size1() {
        // given
        createIssue("first", IssuePriority.P0, 1);
        createIssue("second", IssuePriority.P1, 1);
        createIssue("third", IssuePriority.P2, 1);

        // when
        Page<IssueSummary> page =
                sut.searchByProject(PID, IssueSearchCondition.empty(), PageRequest.of(0, 1), actor.getId());

        // then
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).priority()).isEqualTo(IssuePriority.P0);
    }

    @Test
    @DisplayName("custom sort with storyPoint desc")
    void customSort_storyPointDesc() {
        // given
        createIssue("small", IssuePriority.P2, 1);
        createIssue("medium", IssuePriority.P2, 5);
        createIssue("large", IssuePriority.P2, 13);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("storyPoint")));

        // when
        Page<IssueSummary> page = sut.searchByProject(PID, IssueSearchCondition.empty(), pageable, actor.getId());

        // then
        assertThat(page.getContent()).extracting(IssueSummary::storyPoint).containsExactly(13, 5, 1);
    }

    @Test
    @DisplayName("sort alias 'dueAt' is mapped to schedule.dueAt without error")
    void sortAlias_dueAt() {
        // given
        createIssue("a", IssuePriority.P2, 1);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("dueAt")));

        // when & then
        Page<IssueSummary> page = sut.searchByProject(PID, IssueSearchCondition.empty(), pageable, actor.getId());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("disallowed sort property throws IllegalArgumentException")
    void disallowedSort() {
        // given
        createIssue("a", IssuePriority.P2, 1);
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("someUnknownField")));

        // when / then
        assertThatThrownBy(() -> sut.searchByProject(PID, IssueSearchCondition.empty(), pageable, actor.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("someUnknownField");
    }

    @Test
    @DisplayName("if actor is not project member, is rejected before query")
    void nonMember_rejected() {
        // given
        createIssue("a", IssuePriority.P2, 1);

        // when / then
        assertThatThrownBy(() ->
                        sut.searchByProject(PID, IssueSearchCondition.empty(), PageRequest.of(0, 10), outsider.getId()))
                .isInstanceOf(ProjectMemberNotFoundException.class);
    }
}
