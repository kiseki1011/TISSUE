package com.tissue.common.vo;

import com.tissue.common.enums.ResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Embeddable
@Getter
public class EntityReference {

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private Long id;

    @Column(name = "workspace_key", nullable = false)
    private String workspaceKey;

    @Column(name = "project_key")
    private @Nullable String projectKey;

    @Column(name = "resource_key")
    private @Nullable String key;

    @SuppressWarnings("NullAway.Init")
    protected EntityReference() {}

    @Builder
    private EntityReference(
            ResourceType resourceType,
            Long id,
            String workspaceKey,
            @Nullable String projectKey,
            @Nullable String key) {
        this.resourceType = resourceType;
        this.id = id;
        this.workspaceKey = workspaceKey;
        this.projectKey = projectKey;
        this.key = key;
    }

    public static EntityReference forSprint(
            String workspaceKey, @Nullable String projectKey, String sprintKey, Long sprintId) {
        return EntityReference.builder()
                .resourceType(ResourceType.SPRINT)
                .id(sprintId)
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .key(sprintKey)
                .build();
    }

    public static EntityReference forIssue(String workspaceKey, String projectKey, String issueKey, Long issueId) {
        return EntityReference.builder()
                .resourceType(ResourceType.ISSUE)
                .id(issueId)
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .key(issueKey)
                .build();
    }

    public static EntityReference forIssueComment(
            String workspaceKey, String projectKey, String issueKey, Long commentId) {
        return EntityReference.builder()
                .resourceType(ResourceType.ISSUE_COMMENT)
                .id(commentId)
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .key(issueKey)
                .build();
    }

    public static EntityReference forWorkspace(String workspaceKey, Long workspaceId) {
        return EntityReference.builder()
                .resourceType(ResourceType.WORKSPACE)
                .id(workspaceId)
                .workspaceKey(workspaceKey)
                .build();
    }

    public static EntityReference forWorkspaceMember(String workspaceKey, Long workspaceMemberId) {
        return EntityReference.builder()
                .resourceType(ResourceType.WORKSPACE_MEMBER)
                .id(workspaceMemberId)
                .workspaceKey(workspaceKey)
                .build();
    }
}
