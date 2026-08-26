package com.tissue.feature.activitylog.adapter.persistence;

import com.tissue.feature.activitylog.application.port.repository.ActivityLogQueryRepository;
import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.shared.enums.ResourceType;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        evaluation = Evaluation.ACCEPTABLE,
        evaluationReason = "Passes human written integration test",
        model = "claude-opus-4-8",
        reviewedBy = "kiseki1011")
@Repository
@RequiredArgsConstructor
public class ActivityLogQuerySpecificationAdapter implements ActivityLogQueryRepository {

    private final ActivityLogJpaRepository jpaRepository;

    @Override
    public Map<String, Instant> findLastActivityAtByProjectKeys(Collection<String> projectKeys) {
        if (projectKeys.isEmpty()) {
            return Map.of();
        }
        return jpaRepository.findLastActivityByProjectKeys(projectKeys, ActivityType.issueTypes()).stream()
                .collect(Collectors.toMap(
                        ProjectLastActivityRow::getProjectKey, ProjectLastActivityRow::getLastActivityAt));
    }

    @Override
    public Map<String, Instant> findLastActivityAtByIssueKeys(Collection<String> issueKeys) {
        if (issueKeys.isEmpty()) {
            return Map.of();
        }
        return jpaRepository.findLastActivityByIssueKeys(issueKeys).stream()
                .collect(Collectors.toMap(IssueLastActivityRow::getIssueKey, IssueLastActivityRow::getLastActivityAt));
    }

    @Override
    public List<ActivityLog> findAllByIssueKey(String issueKey, @Nullable Long keysetId, int limit) {
        Specification<ActivityLog> spec = Specification.where(ActivityLogSpecs.hasResourceType(ResourceType.ISSUE))
                .and(ActivityLogSpecs.hasIssueKey(issueKey))
                .and(ActivityLogSpecs.beforeKeyset(keysetId));

        Pageable pageable = createPageable(limit);
        return jpaRepository.findAll(spec, pageable).getContent();
    }

    @Override
    public List<ActivityLog> findAllBySprintId(Long sprintId, @Nullable Long keysetId, int limit) {
        Specification<ActivityLog> spec = Specification.where(ActivityLogSpecs.hasResourceType(ResourceType.SPRINT))
                .and(ActivityLogSpecs.hasResourceId(sprintId))
                .and(ActivityLogSpecs.beforeKeyset(keysetId));

        Pageable pageable = createPageable(limit);
        return jpaRepository.findAll(spec, pageable).getContent();
    }

    @Override
    public Page<ActivityLog> search(
            @Nullable String projectKey,
            @Nullable String issueKey,
            @Nullable Long actorMemberId,
            @Nullable ActivityType type,
            Pageable pageable) {
        Specification<ActivityLog> spec = Specification.where(ActivityLogSpecs.hasProjectKey(projectKey))
                .and(ActivityLogSpecs.matchingIssueKey(issueKey))
                .and(ActivityLogSpecs.hasActor(actorMemberId))
                .and(ActivityLogSpecs.hasActivityType(type));

        return jpaRepository.findAll(spec, pageable);
    }

    private Pageable createPageable(int limit) {
        return PageRequest.of(0, limit, Sort.by("id").descending());
    }
}
