package com.tissue.feature.activitylog.adapter.persistence;

import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.shared.enums.ResourceType;
import jakarta.persistence.criteria.Path;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

public class ActivityLogSpecs {

    private static final String ENTITY_REF = "entityReference";
    private static final String RESOURCE_TYPE = "resourceType";
    private static final String RESOURCE_ID = "resourceId";
    private static final String ISSUE_KEY = "issueKey";
    private static final String PROJECT_KEY = "projectKey";
    private static final String ACTIVITY_TYPE = "activityType";
    private static final String ACTOR_MEMBER_ID = "actorMemberId";
    private static final String ID = "id";

    public static Specification<ActivityLog> hasResourceType(ResourceType type) {
        return (root, query, cb) -> cb.equal(getEntityRefPath(root).get(RESOURCE_TYPE), type);
    }

    public static Specification<ActivityLog> hasResourceId(Long resourceId) {
        return (root, query, cb) -> cb.equal(getEntityRefPath(root).get(RESOURCE_ID), resourceId);
    }

    public static Specification<ActivityLog> hasIssueKey(String issueKey) {
        return (root, query, cb) -> cb.equal(getEntityRefPath(root).get(ISSUE_KEY), issueKey);
    }

    public static @Nullable Specification<ActivityLog> beforeKeyset(@Nullable Long keysetId) {
        if (keysetId == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThan(root.get(ID), keysetId);
    }

    public static @Nullable Specification<ActivityLog> hasProjectKey(@Nullable String projectKey) {
        if (projectKey == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(getEntityRefPath(root).get(PROJECT_KEY), projectKey);
    }

    public static @Nullable Specification<ActivityLog> matchingIssueKey(@Nullable String issueKey) {
        if (issueKey == null) {
            return null;
        }
        return hasIssueKey(issueKey);
    }

    public static @Nullable Specification<ActivityLog> hasActor(@Nullable Long actorMemberId) {
        if (actorMemberId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(ACTOR_MEMBER_ID), actorMemberId);
    }

    public static @Nullable Specification<ActivityLog> hasActivityType(@Nullable ActivityType type) {
        if (type == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(ACTIVITY_TYPE), type);
    }

    private static Path<Object> getEntityRefPath(Path<ActivityLog> root) {
        return root.get(ENTITY_REF);
    }
}
