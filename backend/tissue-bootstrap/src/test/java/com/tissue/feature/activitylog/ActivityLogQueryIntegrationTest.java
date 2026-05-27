package com.tissue.feature.activitylog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.application.port.repository.ActivityLogCommandRepository;
import com.tissue.feature.activitylog.application.service.ActivityLogQueryService;
import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.service.IssueLifecycleService;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issue.domain.exception.IssueNotFoundException;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.exception.ProjectMemberNotFoundException;
import com.tissue.feature.sprint.application.port.repository.SprintCommandRepository;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.sprint.domain.exception.SprintNotFoundException;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.KeysetPageResponse;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.vo.EntityReference;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ActivityLogQueryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ActivityLogQueryService sut;

    @Autowired
    private ActivityLogCommandRepository activityLogCommandRepository;

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

    @Autowired
    private SprintCommandRepository sprintRepository;

    private static final ProjectIdentifier PID = ProjectIdentifier.of("WSP", "PROJ");

    private Member projectMember;
    private Member workspaceOnly;
    private Workspace workspace;
    private Project project;
    private IssueIdentifier issueId;
    private Long sprintId;

    @BeforeEach
    void setUp() {
        projectMember = memberRepository.save(Member.create("pm@test.com", "pm", "PM"));
        workspaceOnly = memberRepository.save(Member.create("ws@test.com", "ws", "WS"));

        workspace = workspaceRepository.save(Workspace.create(PID.workspaceKey(), "Workspace", null));
        project = projectRepository.save(Project.create(workspace, PID.projectKey(), "Project", null));

        WorkspaceMember pmWm =
                workspaceMemberRepository.save(WorkspaceMember.create(projectMember, workspace, WorkspaceRole.OWNER));
        workspaceMemberRepository.save(WorkspaceMember.create(workspaceOnly, workspace, WorkspaceRole.MEMBER));
        projectMemberRepository.save(ProjectMember.createManager(project, pmWm));

        Workflow workflow = Workflow.create(project, Name.of("Default"), null, ColorType.YELLOW);
        workflow.addState(Name.of("TODO"), null, ColorType.GREEN, StateCategory.INITIAL);
        workflow.addState(Name.of("DONE"), null, ColorType.BLACK, StateCategory.COMPLETED);
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

        sprintId =
                sprintRepository.save(Sprint.create(project, "Sprint 1", null)).getId();

        em.flush();
        Long issueTypeId = issueType.getId();
        em.clear();

        CreateIssueCommand cmd = CreateIssueCommand.builder()
                .sprintId(null)
                .parentProjectKey(null)
                .parentKey(null)
                .title("Test")
                .content("c")
                .summary(null)
                .priority(IssuePriority.P2)
                .dueAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .storyPoint(null)
                .issueTypeId(issueTypeId)
                .customFields(Map.of())
                .assigneeMemberId(null)
                .build();
        var response = issueLifecycleService.create(PID, cmd, projectMember.getId());
        em.flush();
        em.clear();
        issueId = IssueIdentifier.of(PID.workspaceKey(), response.issueKey());
    }

    private void seedIssueActivity() {
        ActivityLog log = ActivityLog.builder()
                .eventId(UUID.randomUUID())
                .activityType(ActivityType.ISSUE_CREATED)
                .entityReference(EntityReference.forIssue(workspace.getKey(), project.getKey(), issueId.issueKey()))
                .actorMemberId(projectMember.getId())
                .data(Map.of("test", "data"))
                .build();
        activityLogCommandRepository.save(log);
    }

    private void seedSprintActivity() {
        ActivityLog log = ActivityLog.builder()
                .eventId(UUID.randomUUID())
                .activityType(ActivityType.SPRINT_STARTED)
                .entityReference(EntityReference.forSprint(workspace.getKey(), project.getKey(), sprintId))
                .actorMemberId(projectMember.getId())
                .data(Map.of("test", "data"))
                .build();
        activityLogCommandRepository.save(log);
    }

    @Test
    @DisplayName("getIssueActivities returns logs for a project member")
    void getIssueActivities_success() {
        seedIssueActivity();

        KeysetPageResponse<ActivityLogResponse> response =
                sut.getIssueActivities(issueId, projectMember.getId(), null, 10);

        assertThat(response.content()).isNotEmpty();
    }

    @Test
    @DisplayName("getIssueActivities denied for workspace members who are not in the project")
    void getIssueActivities_deniedForWorkspaceOnly() {
        seedIssueActivity();

        assertThatThrownBy(() -> sut.getIssueActivities(issueId, workspaceOnly.getId(), null, 10))
                .isInstanceOf(ProjectMemberNotFoundException.class);
    }

    @Test
    @DisplayName("getIssueActivities throws when the issue does not exist")
    void getIssueActivities_issueNotFound() {
        assertThatThrownBy(() -> sut.getIssueActivities(
                        IssueIdentifier.of(PID.workspaceKey(), "PROJ-9999"), projectMember.getId(), null, 10))
                .isInstanceOf(IssueNotFoundException.class);
    }

    @Test
    @DisplayName("getSprintActivities returns logs for a project member")
    void getSprintActivities_success() {
        seedSprintActivity();

        KeysetPageResponse<ActivityLogResponse> response =
                sut.getSprintActivities(workspace.getKey(), sprintId, projectMember.getId(), null, 10);

        assertThat(response.content()).isNotEmpty();
    }

    @Test
    @DisplayName("getSprintActivities denied for workspace members who are not in the project")
    void getSprintActivities_deniedForWorkspaceOnly() {
        seedSprintActivity();

        assertThatThrownBy(() -> sut.getSprintActivities(workspace.getKey(), sprintId, workspaceOnly.getId(), null, 10))
                .isInstanceOf(ProjectMemberNotFoundException.class);
    }

    @Test
    @DisplayName("getSprintActivities throws when the sprint does not exist")
    void getSprintActivities_sprintNotFound() {
        assertThatThrownBy(() -> sut.getSprintActivities(workspace.getKey(), 99999L, projectMember.getId(), null, 10))
                .isInstanceOf(SprintNotFoundException.class);
    }
}
