package com.tissue.feature.activitylog.application.service;

import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.application.port.repository.ActivityLogQueryRepository;
import com.tissue.feature.activitylog.application.port.usecase.ActivityLogQueryUseCase;
import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.sprint.application.service.SprintFinder;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.KeysetPageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ActivityLogQueryService implements ActivityLogQueryUseCase {

    private final ActivityLogQueryRepository activityLogQueryRepository;
    private final IssueFinder issueFinder;
    private final SprintFinder sprintFinder;
    private final ProjectMemberFinder projectMemberFinder;

    @Override
    public KeysetPageResponse<ActivityLogResponse> getIssueActivities(
            IssueIdentifier iid, Long actorMemberId, @Nullable Long keysetId, int limit) {
        Issue issue = issueFinder.getWithProjectBy(iid.workspaceKey(), iid.issueKey());
        projectMemberFinder.getBy(issue.getProject(), actorMemberId);

        List<ActivityLog> logs = activityLogQueryRepository.findAllByWorkspaceKeyAndIssueKey(
                iid.workspaceKey(), iid.issueKey(), keysetId, limit);
        return createResponse(logs);
    }

    @Override
    public KeysetPageResponse<ActivityLogResponse> getSprintActivities(
            String workspaceKey, Long sprintId, Long actorMemberId, @Nullable Long keysetId, int limit) {
        Sprint sprint = sprintFinder.getWithProject(workspaceKey, sprintId);
        projectMemberFinder.getBy(sprint.getProject(), actorMemberId);

        List<ActivityLog> logs =
                activityLogQueryRepository.findAllByWorkspaceKeyAndSprintId(workspaceKey, sprintId, keysetId, limit);
        return createResponse(logs);
    }

    private KeysetPageResponse<ActivityLogResponse> createResponse(List<ActivityLog> logs) {
        List<ActivityLogResponse> content =
                logs.stream().map(ActivityLogResponse::from).toList();

        Long nextKeysetId = null;
        if (!content.isEmpty()) {
            nextKeysetId = content.getLast().id();
        }

        return KeysetPageResponse.of(content, nextKeysetId);
    }
}
