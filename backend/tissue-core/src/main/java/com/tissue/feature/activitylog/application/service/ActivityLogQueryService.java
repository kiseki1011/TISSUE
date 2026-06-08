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
import com.tissue.shared.dto.Cursor;
import com.tissue.shared.dto.CursorPage;
import com.tissue.shared.dto.IdCursor;
import com.tissue.shared.dto.IssueIdentifier;
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
    public CursorPage<ActivityLogResponse> getIssueActivities(
            IssueIdentifier iid, Long actorMemberId, @Nullable String cursor, int limit) {
        Issue issue = issueFinder.getWithProjectByIssueKey(iid.issueKey());
        projectMemberFinder.getBy(issue.getProject(), actorMemberId);

        List<ActivityLog> rows =
                activityLogQueryRepository.findAllByIssueKey(iid.issueKey(), decodeKeyset(cursor), limit + 1);
        return toCursorPage(rows, limit);
    }

    @Override
    public CursorPage<ActivityLogResponse> getSprintActivities(
            Long sprintId, Long actorMemberId, @Nullable String cursor, int limit) {
        Sprint sprint = sprintFinder.getWithProject(sprintId);
        projectMemberFinder.getBy(sprint.getProject(), actorMemberId);

        List<ActivityLog> rows =
                activityLogQueryRepository.findAllBySprintId(sprintId, decodeKeyset(cursor), limit + 1);
        return toCursorPage(rows, limit);
    }

    @Nullable
    private Long decodeKeyset(@Nullable String cursor) {
        IdCursor decoded = Cursor.decode(cursor, IdCursor.class);
        return (decoded != null) ? decoded.id() : null;
    }

    private CursorPage<ActivityLogResponse> toCursorPage(List<ActivityLog> rows, int limit) {
        boolean hasNext = rows.size() > limit;
        List<ActivityLog> pageRows = hasNext ? rows.subList(0, limit) : rows;

        List<ActivityLogResponse> content =
                pageRows.stream().map(ActivityLogResponse::from).toList();
        String nextCursor =
                hasNext ? Cursor.encode(new IdCursor(content.getLast().id())) : null;

        return CursorPage.of(content, nextCursor);
    }
}
