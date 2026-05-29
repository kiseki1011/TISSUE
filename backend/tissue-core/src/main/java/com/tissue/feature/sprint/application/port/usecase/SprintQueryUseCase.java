package com.tissue.feature.sprint.application.port.usecase;

import com.tissue.feature.sprint.application.dto.response.SprintDetail;
import com.tissue.feature.sprint.application.dto.response.SprintIssueKeys;
import com.tissue.feature.sprint.application.dto.response.SprintSummary;
import com.tissue.feature.sprint.domain.SprintStatus;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SprintQueryUseCase {

    SprintDetail getSprintDetail(Long sprintId, Long actorMemberId);

    SprintIssueKeys getSprintIssueKeys(Long sprintId, Long actorMemberId);

    /**
     * Paged list of sprints in a project. Optional status filter (e.g. PLANNING + ACTIVE for
     * "current and upcoming"). Default sort suggestion: status priority + startedAt desc.
     */
    Page<SprintSummary> getProjectSprints(
            ProjectIdentifier pid, @Nullable Set<SprintStatus> statuses, Pageable pageable, Long actorMemberId);
}
