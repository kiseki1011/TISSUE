package com.tissue.activitylog.adapter.out.persistence;

import com.tissue.activitylog.application.port.out.ActivityLogQueryRepository;
import com.tissue.activitylog.domain.ActivityLog;
import com.tissue.common.enums.ResourceType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ActivityLogQueryAdapter implements ActivityLogQueryRepository {

    private final EntityManager em;

    @Override
    public List<ActivityLog> findByIssue(String workspaceKey, String issueKey, @Nullable Long cursorId, int limit) {
        return findLogs(workspaceKey, ResourceType.ISSUE, null, issueKey, cursorId, limit);
    }

    @Override
    public List<ActivityLog> findBySprint(String workspaceKey, Long sprintId, @Nullable Long cursorId, int limit) {
        return findLogs(workspaceKey, ResourceType.SPRINT, sprintId, null, cursorId, limit);
    }

    private List<ActivityLog> findLogs(
            String workspaceKey,
            ResourceType resourceType,
            @Nullable Long resourceId,
            @Nullable String resourceKey,
            @Nullable Long cursorId,
            int limit) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ActivityLog> query = cb.createQuery(ActivityLog.class);
        Root<ActivityLog> log = query.from(ActivityLog.class);

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(log.get("entityReference").get("workspaceKey"), workspaceKey));
        predicates.add(cb.equal(log.get("entityReference").get("resourceType"), resourceType));

        if (resourceId != null) {
            predicates.add(cb.equal(log.get("entityReference").get("id"), resourceId));
        } else if (resourceKey != null) {
            predicates.add(cb.equal(log.get("entityReference").get("key"), resourceKey));
        }

        if (cursorId != null) {
            predicates.add(cb.lessThan(log.get("id"), cursorId));
        }

        query.select(log).where(predicates.toArray(new Predicate[0])).orderBy(cb.desc(log.get("id")));

        TypedQuery<ActivityLog> typedQuery = em.createQuery(query);
        typedQuery.setMaxResults(limit);

        return typedQuery.getResultList();
    }
}
