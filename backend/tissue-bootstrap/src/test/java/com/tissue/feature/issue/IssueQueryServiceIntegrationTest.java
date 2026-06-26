package com.tissue.feature.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.adapter.web.IssueQueryController;
import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.dto.response.IssueCommonDetail;
import com.tissue.feature.issue.application.dto.response.IssueCustomDetail;
import com.tissue.feature.issue.application.dto.response.IssueDetail;
import com.tissue.feature.issue.application.dto.response.IssueDetailView;
import com.tissue.feature.issue.application.dto.response.IssueRelationsDetail;
import com.tissue.feature.issue.application.dto.response.IssueReviewersDetail;
import com.tissue.feature.issue.application.dto.response.IssueSubscribersDetail;
import com.tissue.feature.issue.application.dto.response.TransitionDetail;
import com.tissue.feature.issue.application.dto.response.info.IssueBasicInfo;
import com.tissue.feature.issue.application.dto.response.info.IssueIdentifierResponse;
import com.tissue.feature.issue.application.dto.response.info.RelatedIssueInfo;
import com.tissue.feature.issue.application.service.IssueLifecycleService;
import com.tissue.feature.issue.application.service.IssueQueryService;
import com.tissue.feature.issue.application.service.IssueRelationService;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issue.domain.enums.IssueRelationType;
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
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
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
    private IssueQueryController issueQueryController;

    @Autowired
    private IssueLifecycleService issueLifecycleService;

    @Autowired
    private IssueRelationService issueRelationService;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private ProjectCommandRepository projectRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private IssueTypeRepository issueTypeRepository;

    private static final ProjectIdentifier PID = ProjectIdentifier.ofProjectKey("PROJ");

    private Member actor;
    private Member outsider;
    private Long issueTypeId;
    private Long fieldId;

    @BeforeEach
    void setUp() {
        actor = memberRepository.save(Member.create("actor@tissue.com", "actor", "Actor"));
        outsider = memberRepository.save(Member.create("outsider@tissue.com", "outsider", "Outsider"));

        Project project = projectRepository.save(Project.create("PROJ", "Proj", null));
        projectMemberRepository.save(ProjectMember.createManager(project, actor));

        Workflow workflow = Workflow.create(Name.of("Default"), null, ColorType.ANSI_YELLOW);
        WorkflowState todo = workflow.addState(Name.of("TODO"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        WorkflowState inProgress =
                workflow.addState(Name.of("IN PROGRESS"), null, ColorType.ANSI_BLUE, StateCategory.ACTIVE);
        WorkflowState done = workflow.addState(Name.of("DONE"), null, ColorType.ANSI_BLACK, StateCategory.COMPLETED);
        workflow.addTransition(Name.of("Start"), null, todo, inProgress);
        workflow.addTransition(Name.of("Complete"), null, inProgress, done);
        workflowRepository.save(workflow);

        IssueType issueType = IssueType.create(
                Name.of("Story"), null, ColorType.ANSI_RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow);
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
        return IssueIdentifier.ofIssueKey(response.issueKey());
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
        assertThat(detail.customFields().getFirst().fieldLabel()).isEqualTo("goal");
        assertThat(detail.customFields().getFirst().value()).isEqualTo("the goal");
        assertThat(detail.customFields().getFirst().required()).isTrue();
    }

    @Test
    @DisplayName("getDetail returns common fields and custom fields with values in one call")
    void getDetail_success() {
        // given
        IssueIdentifier iid = createIssue("ticket", IssuePriority.P1, Map.of(fieldId, "the goal"));

        // when
        IssueDetail detail = sut.getDetail(iid, actor.getId());

        // then
        assertThat(detail.common().issueKey()).isEqualTo(iid.issueKey());
        assertThat(detail.common().title()).isEqualTo("ticket");
        assertThat(detail.common().priority()).isEqualTo(IssuePriority.P1);
        assertThat(detail.common().assignee().memberId()).isEqualTo(actor.getId());
        assertThat(detail.customFields()).hasSize(1);
        assertThat(detail.customFields().getFirst().fieldLabel()).isEqualTo("goal");
        assertThat(detail.customFields().getFirst().value()).isEqualTo("the goal");
        assertThat(detail.customFields().getFirst().required()).isTrue();
    }

    @Test
    @DisplayName("getDetail rejects if not project member")
    void getDetail_nonMemberRejected() {
        // given
        IssueIdentifier iid = createIssue("ticket", IssuePriority.P2, Map.of(fieldId, "v"));

        // when & then
        assertThatThrownBy(() -> sut.getDetail(iid, outsider.getId()))
                .isInstanceOf(ProjectMemberNotFoundException.class);
    }

    @Test
    @DisplayName(
            "getAvailableTransitions includes each transition's target state, so the client needs no workflow call")
    @LLMGenerated(llmInvolvement = LLMInvolvement.VIBE_CODED, model = "claude-opus-4-8")
    void getAvailableTransitions_includesTargetState() {
        // given
        IssueIdentifier iid = createIssue("ticket", IssuePriority.P2, Map.of(fieldId, "g"));

        // when
        List<TransitionDetail> transitions = sut.getAvailableTransitions(iid, actor.getId());

        // then
        assertThat(transitions).isNotEmpty();
        TransitionDetail start = transitions.getFirst();
        assertThat(start.targetState()).isNotNull();
        assertThat(start.targetState().displayName()).isEqualTo("IN PROGRESS");
        assertThat(start.targetState().color()).isNotNull();
    }

    @Test
    @DisplayName(
            "getIssueDetailView aggregates common, transitions, custom fields, hierarchy, relations, comments in one call")
    @LLMGenerated(llmInvolvement = LLMInvolvement.VIBE_CODED, model = "claude-opus-4-8")
    void getIssueDetailView_returnsAllSections() {
        // given
        IssueIdentifier iid = createIssue("ticket", IssuePriority.P1, Map.of(fieldId, "the goal"));
        MemberDetails actorDetails = new MemberDetails(actor.getId(), actor.getEmail(), actor.getUsername(), List.of());

        // when
        IssueDetailView view = issueQueryController
                .getIssueDetailView(iid.issueKey(), 20, actorDetails)
                .getBody();

        // then
        assertThat(view).isNotNull();
        assertThat(view.common().issueKey()).isEqualTo(iid.issueKey());
        assertThat(view.common().title()).isEqualTo("ticket");
        assertThat(view.common().assignee().memberId()).isEqualTo(actor.getId());

        assertThat(view.availableTransitions()).isNotEmpty();
        assertThat(view.availableTransitions().getFirst().targetState().displayName())
                .isEqualTo("IN PROGRESS");

        assertThat(view.customFields()).hasSize(1);
        assertThat(view.customFields().getFirst().fieldLabel()).isEqualTo("goal");
        assertThat(view.customFields().getFirst().value()).isEqualTo("the goal");
        // A TEXT field has no options, so the resolved option list is empty.
        assertThat(view.customFields().getFirst().options()).isEmpty();

        assertThat(view.parent().issueKey()).isNull();
        assertThat(view.children()).isEmpty();
        assertThat(view.relations().blocks()).isEmpty();
        assertThat(view.comments().content()).isEmpty();
        assertThat(view.comments().totalElements()).isZero();
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
        assertThat(parent.issueType()).isNull();
        assertThat(parent.currentState()).isNull();
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
        assertThat(relations.causes()).isEmpty();
        assertThat(relations.causedBy()).isEmpty();
        assertThat(relations.relevant()).isEmpty();
    }

    @Test
    @DisplayName("getRelations surfaces CAUSES relations (causes for source, causedBy for target)")
    @LLMGenerated(llmInvolvement = LLMInvolvement.VIBE_CODED)
    void getRelations_causes() {
        // given
        IssueIdentifier source = createIssue("cause-src", IssuePriority.P2, Map.of(fieldId, "v"));
        IssueIdentifier target = createIssue("cause-tgt", IssuePriority.P2, Map.of(fieldId, "v"));
        issueRelationService.add(source, target.issueKey(), IssueRelationType.CAUSES, actor.getId());
        em.flush();
        em.clear();

        // when
        IssueRelationsDetail fromSource = sut.getRelations(source, actor.getId());
        IssueRelationsDetail fromTarget = sut.getRelations(target, actor.getId());

        // then
        assertThat(fromSource.causes()).extracting(RelatedIssueInfo::issueKey).containsExactly(target.issueKey());
        assertThat(fromSource.causedBy()).isEmpty();
        assertThat(fromTarget.causedBy()).extracting(RelatedIssueInfo::issueKey).containsExactly(source.issueKey());
        assertThat(fromTarget.causes()).isEmpty();
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
        assertThat(transitions.getFirst().displayLabel()).isEqualTo("Start");
    }
}
