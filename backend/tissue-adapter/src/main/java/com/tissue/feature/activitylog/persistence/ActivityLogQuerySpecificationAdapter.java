package com.tissue.feature.activitylog.persistence;

import com.tissue.feature.activitylog.application.port.repository.ActivityLogQueryRepository;
import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.shared.enums.ResourceType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ActivityLogQuerySpecificationAdapter implements ActivityLogQueryRepository {

    private final ActivityLogJpaRepository jpaRepository;

    @Override
    public List<ActivityLog> findAllByWorkspaceKeyAndIssueKey(
            String workspaceKey, String issueKey, @Nullable Long keysetId, int limit) {
        Specification<ActivityLog> spec = Specification.where(ActivityLogSpecs.hasWorkspace(workspaceKey))
                .and(ActivityLogSpecs.hasResourceType(ResourceType.ISSUE))
                .and(ActivityLogSpecs.hasIssueKey(issueKey))
                .and(ActivityLogSpecs.beforeKeyset(keysetId));

        Pageable pageable = createPageable(limit);
        return jpaRepository.findAll(spec, pageable).getContent();
    }

    @Override
    public List<ActivityLog> findAllByWorkspaceKeyAndSprintId(
            String workspaceKey, Long sprintId, @Nullable Long keysetId, int limit) {
        Specification<ActivityLog> spec = Specification.where(ActivityLogSpecs.hasWorkspace(workspaceKey))
                .and(ActivityLogSpecs.hasResourceType(ResourceType.SPRINT))
                .and(ActivityLogSpecs.hasResourceId(sprintId))
                .and(ActivityLogSpecs.beforeKeyset(keysetId));

        Pageable pageable = createPageable(limit);
        return jpaRepository.findAll(spec, pageable).getContent();
    }

    private Pageable createPageable(int limit) {
        return PageRequest.of(0, limit, Sort.by("id").descending());
    }
}
