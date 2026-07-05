package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.Set;
import org.springframework.data.domain.Page;

public interface IssueTrashUseCase {

    /**
     * Lists a project's soft-deleted issues for the trash view.
     *
     * <p>Newest deletion first. An empty {@code authorMemberIds} returns every deleted issue.
     * Otherwise, only those authored by the given members.
     */
    Page<IssueSummary> listDeletedByProject(
            ProjectIdentifier pid, Set<Long> authorMemberIds, int page, int size, Long actorMemberId);
}
