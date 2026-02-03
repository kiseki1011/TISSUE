package com.tissue.activitylog;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.activitylog.application.port.out.ActivityLogRepository;
import com.tissue.activitylog.domain.ActivityLog;
import com.tissue.activitylog.domain.ActivityType;
import com.tissue.common.dto.FieldChange;
import com.tissue.issue.domain.event.IssueCreatedEvent;
import com.tissue.issue.domain.event.IssueFieldsUpdatedEvent;
import com.tissue.issue.domain.event.IssueTransitionedEvent;
import com.tissue.member.application.port.out.MemberCommandRepository;
import com.tissue.member.domain.Member;
import com.tissue.project.application.port.out.ProjectCommandRepository;
import com.tissue.project.application.port.out.ProjectMemberCommandRepository;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.support.IntegrationTestSupport;
import com.tissue.workspace.application.port.out.WorkspaceCommandRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberCommandRepository;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

class ActivityLogIntegrationTest extends IntegrationTestSupport {

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    ActivityLogRepository activityLogRepository;

    @Autowired
    MemberCommandRepository memberCommandRepository;

    @Autowired
    WorkspaceCommandRepository workspaceCommandRepository;

    @Autowired
    WorkspaceMemberCommandRepository workspaceMemberCommandRepository;

    @Autowired
    ProjectCommandRepository projectCommandRepository;

    @Autowired
    ProjectMemberCommandRepository projectMemberCommandRepository;

    private Member actor;
    private Workspace workspace;
    private Project project;

    @BeforeEach
    void setupData() {
        // create actor Member
        actor = Member.create("actor@test.com", "actor", "Actor");
        actor = memberCommandRepository.save(actor);

        // create Workspace
        workspace = Workspace.create("TEST-WS", "Test Workspace", "Test Description");
        workspace = workspaceCommandRepository.save(workspace);

        // add Member(actor) to Workspace
        WorkspaceMember actorWsMember = WorkspaceMember.create(actor, workspace, WorkspaceRole.OWNER);
        actorWsMember = workspaceMemberCommandRepository.save(actorWsMember);

        // create Project
        project = Project.create(workspace, "TEST", "Test Project", "Test Description");
        project = projectCommandRepository.save(project);

        // add Member(actor) to Project
        ProjectMember actorProjectMember = ProjectMember.create(project, actorWsMember);
        projectMemberCommandRepository.save(actorProjectMember);
    }

    @Test
    @DisplayName("Activity log created when IssueCreatedEvent occurs")
    void handleIssueCreated() {
        Long issueId = 100L;
        String issueKey = "TEST-1";
        IssueCreatedEvent event = IssueCreatedEvent.create(
                workspace.getKey(),
                project.getKey(),
                project.getId(),
                issueKey,
                issueId,
                null,
                null,
                actor.getId(),
                actor.getUsername());

        publisher.publishEvent(event);

        List<ActivityLog> logs = activityLogRepository.findAll();
        assertThat(logs).hasSize(1);

        ActivityLog log = logs.get(0);
        assertThat(log.getActivityType()).isEqualTo(ActivityType.ISSUE_CREATED);
        assertThat(log.getActorMemberId()).isEqualTo(actor.getId());
        assertThat(log.getEntityReference().getIssueKey()).isEqualTo(issueKey);
        assertThat(log.getData().get("issueKey")).isEqualTo(issueKey);
    }

    @Test
    @DisplayName("Activity log with changes is created when IssueFieldsUpdatedEvent occurs")
    void handleIssueUpdated() {
        Long issueId = 100L;
        String issueKey = "TEST-1";
        Map<String, FieldChange> changes = Map.of(
                "title", new FieldChange("Old Title", "New Title"),
                "priority", new FieldChange("LOW", "HIGH"));

        IssueFieldsUpdatedEvent event = IssueFieldsUpdatedEvent.create(
                workspace.getKey(), project.getKey(), issueKey, issueId, changes, actor.getId(), actor.getUsername());

        publisher.publishEvent(event);

        List<ActivityLog> logs = activityLogRepository.findAll();
        assertThat(logs).hasSize(1);

        ActivityLog log = logs.get(0);
        assertThat(log.getActivityType()).isEqualTo(ActivityType.ISSUE_UPDATED);
        assertThat(log.getChanges()).isNotNull();

        assertThat(log.getChanges()).containsKey("title");
        assertThat(log.getChanges().get("title").from()).isEqualTo("Old Title");
        assertThat(log.getChanges().get("title").to()).isEqualTo("New Title");
    }

    @Test
    @DisplayName("Activity log is created when IssueTransitionedEvent occurs")
    void handleIssueTransitioned() {
        Long issueId = 100L;
        String issueKey = "TEST-1";
        String oldState = "To Do";
        String newState = "In Progress";

        IssueTransitionedEvent event = IssueTransitionedEvent.create(
                workspace.getKey(),
                project.getKey(),
                issueKey,
                issueId,
                null,
                null,
                1L,
                "Start Progress",
                1L,
                oldState,
                2L,
                newState,
                actor.getId(),
                actor.getUsername());

        publisher.publishEvent(event);

        List<ActivityLog> logs = activityLogRepository.findAll();
        assertThat(logs).hasSize(1);

        ActivityLog log = logs.get(0);
        assertThat(log.getActivityType().name()).isEqualTo("ISSUE_WORKFLOW_TRANSITIONED");

        assertThat(log.getData().get("oldState")).isEqualTo(oldState);
        assertThat(log.getData().get("newState")).isEqualTo(newState);

        assertThat(log.getChanges()).containsKey("state");
        assertThat(log.getChanges().get("state").from()).isEqualTo(oldState);
        assertThat(log.getChanges().get("state").to()).isEqualTo(newState);
    }
}
