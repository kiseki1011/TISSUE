package com.tissue.feature.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.dto.response.IssueCommonDetail;
import com.tissue.feature.issue.application.dto.response.IssueCustomDetail;
import com.tissue.feature.issue.application.dto.response.IssueRelationsDetail;
import com.tissue.feature.issue.application.dto.response.IssueReviewersDetail;
import com.tissue.feature.issue.application.dto.response.IssueSubscribersDetail;
import com.tissue.feature.issue.application.dto.response.TransitionDetail;
import com.tissue.feature.issue.application.dto.response.info.IssueBasicInfo;
import com.tissue.feature.issue.application.dto.response.info.IssueIdentifierResponse;
import com.tissue.feature.issue.application.service.IssueLifecycleService;
import com.tissue.feature.issue.application.service.IssueQueryService;
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
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class IssueQueryServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private IssueQueryService sut;

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

    private IssueIdentifier createIssue(String title, IssuePriority priority, Map<Long, Object> customFields) {
        CreateIssueCommand cmd = CreateIssueCommand.builder()
                .sprintId(null)
                .parentProjectKey(null)
                .parentKey(null)
                .title(title)
                .content("c")
                .summary("s")
                .priority(priority)
                .dueAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .storyPoint(3)
                .issueTypeId(issueTypeId)
                .customFields(customFields)
                .assigneeMemberId(actor.getId())
                .build();
        var response = issueLifecycleService.create(PID, cmd, actor.getId());
        em.flush();
        em.clear();
        return IssueIdentifier.of(PID.workspaceKey(), response.issueKey());
    }

    @Test
    @DisplayName("getBasic returns title, type, priority, current state for the issue author")
    void getBasic_success() {
        // given
        IssueIdentifier iid = createIssue("first", IssuePriority.P2, Map.of(fieldId, "goal value"));

        // when
        IssueBasicInfo info = sut.getBasic(iid, actor.getId());

        // then
        assertThat(info.issueKey()).isEqualTo(iid.issueKey());
        assertThat(info.title()).isEqualTo("first");
        assertThat(info.priority()).isEqualTo(IssuePriority.P2);
        assertThat(info.currentState().displayName()).isEqualTo("TODO");
        assertThat(info.author().memberId()).isEqualTo(actor.getId());
    }

    @Test
    @DisplayName("getBasic rejects if not project member")
    void getBasic_nonMemberRejected() {
        // given
        IssueIdentifier iid = createIssue("first", IssuePriority.P2, Map.of(fieldId, "goal value"));

        // when & then
        assertThatThrownBy(() -> sut.getBasic(iid, outsider.getId()))
                .isInstanceOf(ProjectMemberNotFoundException.class);
    }

    @Test
    @DisplayName("getCommonFieldValues returns full common fields, assignee, reviewers list")
    void getCommonFieldValues_success() {
        // given
        IssueIdentifier iid = createIssue("ticket", IssuePriority.P1, Map.of(fieldId, "g"));

        // when
        IssueCommonDetail detail = sut.getCommonFieldValues(iid, actor.getId());

        // then
        assertThat(detail.issueKey()).isEqualTo(iid.issueKey());
        assertThat(detail.title()).isEqualTo("ticket");
        assertThat(detail.priority()).isEqualTo(IssuePriority.P1);
        assertThat(detail.assignee().memberId()).isEqualTo(actor.getId());
        assertThat(detail.reviewers()).isEmpty();
        assertThat(detail.subscribersCount()).isZero();
    }

    @Test
    @DisplayName("getCustomFieldValues includes the custom field with its value")
    void getCustomFieldValues_success() {
        // given
        IssueIdentifier iid = createIssue("ticket", IssuePriority.P2, Map.of(fieldId, "the goal"));

        // when
        IssueCustomDetail detail = sut.getCustomFieldValues(iid, actor.getId());

        // then
        assertThat(detail.customFields()).hasSize(1);
        assertThat(detail.customFields().get(0).fieldLabel()).isEqualTo("goal");
        assertThat(detail.customFields().get(0).value()).isEqualTo("the goal");
        assertThat(detail.customFields().get(0).required()).isTrue();
    }

    @Test
    @DisplayName("getChildren returns empty for issues without children")
    void getChildren_emptyList() {
        // given
        IssueIdentifier iid = createIssue("standalone", IssuePriority.P3, Map.of(fieldId, "v"));

        // when
        List<IssueIdentifierResponse> children = sut.getChildren(iid, actor.getId());

        // then
        assertThat(children).isEmpty();
    }

    @Test
    @DisplayName("getParent returns null when no parent is set")
    void getParent_noParent() {
        // given
        IssueIdentifier iid = createIssue("orphan", IssuePriority.P3, Map.of(fieldId, "v"));

        // when
        IssueIdentifierResponse parent = sut.getParent(iid, actor.getId());

        // then
        assertThat(parent.issueKey()).isNull();
        assertThat(parent.issueTypeLabel()).isNull();
    }

    @Test
    @DisplayName("getRelations returns empty collections when no relations exist")
    void getRelations_empty() {
        // given
        IssueIdentifier iid = createIssue("solo", IssuePriority.P2, Map.of(fieldId, "v"));

        // when
        IssueRelationsDetail relations = sut.getRelations(iid, actor.getId());

        // then
        assertThat(relations.blocks()).isEmpty();
        assertThat(relations.blockedBy()).isEmpty();
        assertThat(relations.duplicates()).isEmpty();
        assertThat(relations.duplicatedBy()).isEmpty();
        assertThat(relations.relevant()).isEmpty();
    }

    @Test
    @DisplayName("getReviewers / getSubscribers return empty collections when no reviewer/subscriber exist")
    void getReviewersAndSubscribers_empty() {
        // given
        IssueIdentifier iid = createIssue("fresh", IssuePriority.P2, Map.of(fieldId, "v"));

        // when
        IssueReviewersDetail reviewers = sut.getReviewers(iid, actor.getId());
        IssueSubscribersDetail subscribers = sut.getSubscribers(iid, actor.getId());

        // then
        assertThat(reviewers.reviewers()).isEmpty();
        assertThat(reviewers.totalCount()).isZero();
        assertThat(subscribers.subscribers()).isEmpty();
        assertThat(subscribers.totalCount()).isZero();
    }

    @Test
    @DisplayName("getAvailableTransitions returns outgoing transitions from current state")
    void getAvailableTransitions_fromInitial() {
        // given
        IssueIdentifier iid = createIssue("flow", IssuePriority.P2, Map.of(fieldId, "v"));

        // when
        List<TransitionDetail> transitions = sut.getAvailableTransitions(iid, actor.getId());

        // then
        assertThat(transitions).hasSize(1);
        assertThat(transitions.get(0).displayLabel()).isEqualTo("Start");
    }
}
