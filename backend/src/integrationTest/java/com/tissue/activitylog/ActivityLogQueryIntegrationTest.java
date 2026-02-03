package com.tissue.activitylog;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.activitylog.application.port.out.ActivityLogRepository;
import com.tissue.activitylog.application.service.ActivityLogQueryService;
import com.tissue.activitylog.domain.ActivityLog;
import com.tissue.activitylog.domain.ActivityType;
import com.tissue.common.dto.CursorPageResponse;
import com.tissue.global.vo.EntityReference;
import com.tissue.member.application.port.out.MemberCommandRepository;
import com.tissue.member.domain.Member;
import com.tissue.project.application.dto.ProjectMemberContext;
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
    private ProjectMemberContext actorContext;

    @BeforeEach
    void setupData() {
        // create Member(actor)
        actor = Member.create("actor@test.com", "actor", "Actor");
        actor = memberCommandRepository.save(actor);

        // create Workspace
        workspace = Workspace.create("TEST-WS", "Test Workspace", "Test Description");
        workspace = workspaceCommandRepository.save(workspace);

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
                actorWsMember.getRole());
    }

    @Test
    @DisplayName("Get issue activities successfully")
    void getIssueActivities() {
        Long issueId = 100L;
        String issueKey = "TEST-1";

        ActivityLog log1 = ActivityLog.builder()
                .eventId(UUID.randomUUID())
                .activityType(ActivityType.ISSUE_CREATED)
                .entityReference(EntityReference.forIssue(workspace.getKey(), project.getKey(), issueKey, issueId))
                .actorMemberId(actor.getId())
                .data(Map.of("test", "data1"))
                .build();
        activityLogRepository.save(log1);

        ActivityLog log2 = ActivityLog.builder()
                .eventId(UUID.randomUUID())
                .activityType(ActivityType.ISSUE_UPDATED)
                .entityReference(EntityReference.forIssue(workspace.getKey(), project.getKey(), issueKey, issueId))
                .actorMemberId(actor.getId())
                .data(Map.of("test", "data2"))
                .build();
        activityLogRepository.save(log2);

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
        activityLogRepository.save(log1);

        CursorPageResponse<ActivityLogResponse> response =
                queryService.getSprintActivities(actorContext, sprintId, null, 10);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(log1.getId());
    }
}
