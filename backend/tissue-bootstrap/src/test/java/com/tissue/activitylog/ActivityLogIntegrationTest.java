package com.tissue.activitylog;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.activitylog.application.port.repository.ActivityLogCommandRepository;
import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.feature.issue.domain.event.IssueCreatedEvent;
import com.tissue.feature.issue.domain.event.IssueFieldsUpdatedEvent;
import com.tissue.feature.issue.domain.event.IssueTransitionedEvent;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.dto.FieldChange;
import com.tissue.support.IntegrationTestSupport;
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
    ActivityLogCommandRepository activityLogCommandRepository;

    @Autowired
    MemberCommandRepository memberCommandRepository;

    @Autowired
    WorkspaceRepository workspaceRepository;

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
        workspace = workspaceRepository.save(workspace);

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
        String issueKey = "TEST-1";
        IssueCreatedEvent event = IssueCreatedEvent.create(
                workspace.getKey(), project.getKey(), issueKey, null, actor.getId(), actor.getUsername());

        publisher.publishEvent(event);

        List<ActivityLog> logs = activityLogCommandRepository.findAll();
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
        String issueKey = "TEST-1";
        Map<String, FieldChange> changes = Map.of(
                "title", new FieldChange("Old Title", "New Title"),
                "priority", new FieldChange("LOW", "HIGH"));

        IssueFieldsUpdatedEvent event = IssueFieldsUpdatedEvent.create(
                workspace.getKey(), project.getKey(), issueKey, changes, actor.getId(), actor.getUsername());

        publisher.publishEvent(event);

        List<ActivityLog> logs = activityLogCommandRepository.findAll();
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
        String issueKey = "TEST-1";
        String oldState = "To Do";
        String newState = "In Progress";

        IssueTransitionedEvent event = IssueTransitionedEvent.create(
                workspace.getKey(),
                project.getKey(),
                issueKey,
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

        List<ActivityLog> logs = activityLogCommandRepository.findAll();
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
