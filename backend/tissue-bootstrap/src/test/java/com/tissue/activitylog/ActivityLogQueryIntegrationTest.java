package com.tissue.activitylog;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.application.port.repository.ActivityLogCommandRepository;
import com.tissue.feature.activitylog.application.service.ActivityLogQueryService;
import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.ProjectMemberContext;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.dto.CursorPageResponse;
import com.tissue.shared.vo.EntityReference;
import com.tissue.support.IntegrationTestSupport;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ActivityLogQueryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    ActivityLogQueryService queryService;

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
    private ProjectMemberContext actorContext;

    @BeforeEach
    void setupData() {
        // create Member(actor)
        actor = Member.create("actor@test.com", "actor", "Actor");
        actor = memberCommandRepository.save(actor);

        // create Workspace
        workspace = Workspace.create("TEST-WS", "Test Workspace", "Test Description");
        workspace = workspaceRepository.save(workspace);

        // add Actor to Workspace
        WorkspaceMember actorWsMember = WorkspaceMember.create(actor, workspace, WorkspaceRole.OWNER);
        actorWsMember = workspaceMemberCommandRepository.save(actorWsMember);

        // create Project
        project = Project.create(workspace, "TEST", "Test Project", "Test Description");
        project = projectCommandRepository.save(project);

        // add Member(actor) to Project
        ProjectMember actorProjectMember = ProjectMember.create(project, actorWsMember);
        projectMemberCommandRepository.save(actorProjectMember);

        actorContext = new ProjectMemberContext(
                actorProjectMember.getId(),
                actor.getId(),
                workspace.getId(),
                workspace.getKey(),
                project.getId(),
                project.getKey(),
                actorWsMember.getDisplayName(),
                actorWsMember.getRole(),
                ProjectRole.MEMBER);
    }

    @Test
    @DisplayName("Get issue activities successfully")
    void getIssueActivities() {
        String issueKey = "TEST-1";

        ActivityLog log1 = ActivityLog.builder()
                .eventId(UUID.randomUUID())
                .activityType(ActivityType.ISSUE_CREATED)
                .entityReference(EntityReference.forIssue(workspace.getKey(), project.getKey(), issueKey))
                .actorMemberId(actor.getId())
                .data(Map.of("test", "data1"))
                .build();
        activityLogCommandRepository.save(log1);

        ActivityLog log2 = ActivityLog.builder()
                .eventId(UUID.randomUUID())
                .activityType(ActivityType.ISSUE_UPDATED)
                .entityReference(EntityReference.forIssue(workspace.getKey(), project.getKey(), issueKey))
                .actorMemberId(actor.getId())
                .data(Map.of("test", "data2"))
                .build();
        activityLogCommandRepository.save(log2);

        CursorPageResponse<ActivityLogResponse> response =
                queryService.getIssueActivities(actorContext, issueKey, null, 10);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).id()).isEqualTo(log2.getId());
        assertThat(response.content().get(1).id()).isEqualTo(log1.getId());
    }

    @Test
    @DisplayName("Get sprint activities successfully")
    void getSprintActivities() {
        Long sprintId = 200L;

        ActivityLog log1 = ActivityLog.builder()
                .eventId(UUID.randomUUID())
                .activityType(ActivityType.SPRINT_STARTED)
                .entityReference(EntityReference.forSprint(workspace.getKey(), project.getKey(), sprintId))
                .actorMemberId(actor.getId())
                .data(Map.of("test", "Sprint 1"))
                .build();
        activityLogCommandRepository.save(log1);

        CursorPageResponse<ActivityLogResponse> response =
                queryService.getSprintActivities(actorContext, sprintId, null, 10);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(log1.getId());
    }
}
