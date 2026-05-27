package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.domain.exception.InvalidSortPropertyException;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.sprint.application.port.repository.SprintQueryRepository;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.sprint.domain.SprintStatus;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Search-time policy used by {@link IssueFullTextSearchService}.
 *
 * <p>Provides:
 * <ul>
 *   <li>{@code currentSprintOnly=true} convenience flag → resolved to the project's
 *       active sprint id (or {@code -1L} sentinel when no active sprint exists)</li>
 *   <li>Default sort when the caller does not specify one</li>
 *   <li>Sort property aliasing ({@code dueAt} → {@code schedule.dueAt}) and
 *       whitelist validation (rejects properties that are not safe to sort on)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class IssueSearchPolicy {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.asc("priority"))
            .and(Sort.by(Sort.Order.asc("schedule.dueAt")))
            .and(Sort.by(Sort.Order.desc("id")));

    private static final Map<String, String> SORT_ALIASES = Map.of("dueAt", "schedule.dueAt");

    private static final Set<String> ALLOWED_SORT_PROPERTIES =
            Set.of("priority", "createdAt", "lastModifiedAt", "schedule.dueAt");

    private final SprintQueryRepository sprintQueryRepository;

    /**
     * If {@code currentSprintOnly=true}, looks up the project's active sprint and
     * folds its id into {@code condition.sprintIds}. When no active sprint exists,
     * a sentinel id {@code -1L} is added so the resulting query returns 0 rows
     * instead of silently dropping the filter.
     */
    public IssueSearchCondition resolveCurrentSprint(IssueSearchCondition c, Project project) {
        if (c.currentSprintOnly() == null || !c.currentSprintOnly()) {
            return c;
        }
        Optional<Sprint> activeSprint = sprintQueryRepository.findByProjectAndStatus(project, SprintStatus.ACTIVE);
        Set<Long> sprintIds = new LinkedHashSet<>(c.sprintIds() == null ? Set.of() : c.sprintIds());
        activeSprint.ifPresent(s -> sprintIds.add(s.getId()));
        if (sprintIds.isEmpty()) {
            sprintIds.add(-1L);
        }
        return new IssueSearchCondition(
                c.priorities(),
                c.stateCategories(),
                c.currentStateIds(),
                c.tagIds(),
                c.authorMemberIds(),
                c.assigneeMemberIds(),
                c.reviewerMemberIds(),
                c.subscriberMemberIds(),
                new HashSet<>(sprintIds),
                c.currentSprintOnly(),
                c.dueAtFrom(),
                c.dueAtTo(),
                c.keyword());
    }

    /**
     * Applies the default sort when the caller did not specify one, and maps
     * user-friendly aliases (e.g. {@code dueAt}) to the embedded JPA path
     * ({@code schedule.dueAt}). Throws {@link InvalidSortPropertyException} for
     * any sort property that is not on the whitelist.
     */
    public Pageable applyDefaultSort(Pageable pageable) {
        if (!pageable.getSort().isSorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);
        }
        List<Sort.Order> resolved = pageable.getSort().stream()
                .map(order -> {
                    String prop = SORT_ALIASES.getOrDefault(order.getProperty(), order.getProperty());
                    if (!ALLOWED_SORT_PROPERTIES.contains(prop)) {
                        throw new InvalidSortPropertyException(order.getProperty(), ALLOWED_SORT_PROPERTIES);
                    }
                    return new Sort.Order(order.getDirection(), prop);
                })
                .toList();
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(resolved));
    }
}
