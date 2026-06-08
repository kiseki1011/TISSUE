package com.tissue.feature.issue;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.feature.issue.application.service.IssueLifecycleService;
import com.tissue.feature.issue.application.service.IssueListQueryService;
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
import com.tissue.feature.sprint.application.dto.request.CreateSprintCommand;
import com.tissue.feature.sprint.application.service.SprintCommandService;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.dto.CursorPage;
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
class IssueListQueryServiceIntegrationTest extends IntegrationTestSupport {

    private static final ProjectIdentifier PROJ = ProjectIdentifier.ofProjectKey("PROJ");
    private static final ProjectIdentifier TEAM = ProjectIdentifier.ofProjectKey("TEAM");

    @Autowired
    private IssueListQueryService sut;

    @Autowired
    private IssueLifecycleService issueLifecycleService;

    @Autowired
    private SprintCommandService sprintCommandService;

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

    private Member actor;
    private Member other;
    private Long issueTypeId;
    private Long fieldId;

    @BeforeEach
    void setUp() {
        actor = memberRepository.save(Member.create("actor@tissue.com", "actor", "Actor"));
        other = memberRepository.save(Member.create("other@tissue.com", "other", "Other"));

        Project proj = projectRepository.save(Project.create("PROJ", "Proj", null));
        projectMemberRepository.save(ProjectMember.createManager(proj, actor));
        projectMemberRepository.save(ProjectMember.create(proj, other));

        Project team = projectRepository.save(Project.create("TEAM", "Team", null));
        projectMemberRepository.save(ProjectMember.create(team, actor));

        Workflow workflow = Workflow.create(Name.of("Default"), null, ColorType.YELLOW);
        WorkflowState todo = workflow.addState(Name.of("TODO"), null, ColorType.GREEN, StateCategory.INITIAL);
        WorkflowState inProgress =
                workflow.addState(Name.of("IN PROGRESS"), null, ColorType.BLUE, StateCategory.ACTIVE);
        WorkflowState done = workflow.addState(Name.of("DONE"), null, ColorType.BLACK, StateCategory.COMPLETED);
        workflow.addTransition(Name.of("Start"), null, todo, inProgress);
        workflow.addTransition(Name.of("Complete"), null, inProgress, done);
        workflowRepository.save(workflow);

        IssueType issueType = IssueType.create(
                Name.of("Story"), null, ColorType.RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow);
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

    private String createIssue(ProjectIdentifier pid, String title, Long assigneeMemberId) {
        CreateIssueCommand cmd = CreateIssueCommand.builder()
                .sprintId(null)
                .parentProjectKey(null)
                .parentKey(null)
                .title(title)
                .content("c")
                .summary("s")
                .priority(IssuePriority.P3)
                .dueAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .storyPoint(3)
                .issueTypeId(issueTypeId)
                .customFields(Map.of(fieldId, "v"))
                .assigneeMemberId(assigneeMemberId)
                .build();
        var response = issueLifecycleService.create(pid, cmd, actor.getId());
        em.flush();
        em.clear();
        return response.issueKey();
    }

    private Long createSprintWith(ProjectIdentifier pid, String title, String issueKey) {
        Long sprintId = sprintCommandService
                .createSprint(pid, new CreateSprintCommand(title, null), actor.getId())
                .sprintId();
        sprintCommandService.addIssues(sprintId, List.of(issueKey), actor.getId());
        em.flush();
        em.clear();
        return sprintId;
    }

    @Test
    @DisplayName("returns issues assigned to me within a specific project")
    void getMyWork_scopedToProject() {
        // given
        String mineInProj = createIssue(PROJ, "mine-proj", actor.getId());
        createIssue(TEAM, "mine-team", actor.getId());
        createIssue(PROJ, "theirs", other.getId());

        // when
        CursorPage<IssueSummary> page = sut.getMyWork(PROJ, actor.getId(), null, 20);

        // then
        assertThat(page.content()).extracting(IssueSummary::issueKey).containsExactly(mineInProj);
    }

    @Test
    @DisplayName("returns no-sprint issues and excludes issues already in a sprint")
    void getBacklog_excludesSprintIssues() {
        // given
        String backlogIssue = createIssue(PROJ, "backlog", actor.getId());
        String scheduledIssue = createIssue(PROJ, "scheduled", actor.getId());
        createSprintWith(PROJ, "S1", scheduledIssue);

        // when
        CursorPage<IssueSummary> page = sut.getBacklog(PROJ, actor.getId(), null, 20);

        // then
        assertThat(page.content()).extracting(IssueSummary::issueKey).containsExactly(backlogIssue);
    }

    @Test
    @DisplayName("returns empty when there is no active sprint")
    void getCurrentSprintIssues_emptyWhenNoActiveSprint() {
        // given
        createIssue(PROJ, "loose", actor.getId());

        // when & then
        assertThat(sut.getCurrentSprintIssues(PROJ, actor.getId(), null, 20).content())
                .isEmpty();
    }

    @Test
    @DisplayName("returns the active sprint's issues")
    void getCurrentSprintIssues_active() {
        // given
        String inSprint = createIssue(PROJ, "in-sprint", actor.getId());
        Long sprintId = createSprintWith(PROJ, "S1", inSprint);
        sprintCommandService.start(sprintId, Instant.now().plus(7, ChronoUnit.DAYS), actor.getId());
        em.flush();
        em.clear();

        // when
        CursorPage<IssueSummary> page = sut.getCurrentSprintIssues(PROJ, actor.getId(), null, 20);

        // then
        assertThat(page.content()).extracting(IssueSummary::issueKey).containsExactly(inSprint);
    }
}
