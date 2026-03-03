package com.tissue.feature.activitylog.persistence;

import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.shared.enums.ResourceType;
import jakarta.persistence.criteria.Path;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

public class ActivityLogSpecs {

    private static final String ENTITY_REF = "entityReference";
    private static final String WORKSPACE_KEY = "workspaceKey";
    private static final String RESOURCE_TYPE = "resourceType";
    private static final String RESOURCE_ID = "resourceId";
    private static final String ISSUE_KEY = "issueKey";
    private static final String ID = "id";

    public static Specification<ActivityLog> hasWorkspace(String workspaceKey) {
        return (root, query, cb) -> cb.equal(getEntityRefPath(root).get(WORKSPACE_KEY), workspaceKey);
    }

    public static Specification<ActivityLog> hasResourceType(ResourceType type) {
        return (root, query, cb) -> cb.equal(getEntityRefPath(root).get(RESOURCE_TYPE), type);
    }

    public static Specification<ActivityLog> hasResourceId(Long resourceId) {
        return (root, query, cb) -> cb.equal(getEntityRefPath(root).get(RESOURCE_ID), resourceId);
    }

    public static Specification<ActivityLog> hasIssueKey(String issueKey) {
        return (root, query, cb) -> cb.equal(getEntityRefPath(root).get(ISSUE_KEY), issueKey);
    }

    public static @Nullable Specification<ActivityLog> beforeCursor(@Nullable Long cursorId) {
        if (cursorId == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThan(root.get(ID), cursorId);
    }

    private static Path<Object> getEntityRefPath(Path<ActivityLog> root) {
        return root.get(ENTITY_REF);
    }
}
