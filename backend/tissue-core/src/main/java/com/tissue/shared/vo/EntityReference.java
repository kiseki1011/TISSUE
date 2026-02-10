package com.tissue.shared.vo;

import com.tissue.shared.enums.ResourceType;
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

    @Nullable
    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "workspace_key", nullable = false)
    private String workspaceKey;

    @Column(name = "project_key")
    private @Nullable String projectKey;

    @Column(name = "issue_key")
    private @Nullable String issueKey;

    @Column(name = "member_id")
    private @Nullable Long memberId;

    @SuppressWarnings("NullAway.Init")
    protected EntityReference() {}

    @Builder
    private EntityReference(
            ResourceType resourceType,
            @Nullable Long resourceId,
            String workspaceKey,
            @Nullable String projectKey,
            @Nullable String issueKey,
            @Nullable Long memberId) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.workspaceKey = workspaceKey;
        this.projectKey = projectKey;
        this.issueKey = issueKey;
        this.memberId = memberId;
    }

    public static EntityReference forSprint(String workspaceKey, @Nullable String projectKey, Long sprintId) {
        return EntityReference.builder()
                .resourceType(ResourceType.SPRINT)
                .resourceId(sprintId)
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .build();
    }

    public static EntityReference forIssue(String workspaceKey, String projectKey, String issueKey) {
        return EntityReference.builder()
                .resourceType(ResourceType.ISSUE)
                .resourceId(null)
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .issueKey(issueKey)
                .build();
    }

    public static EntityReference forIssueComment(
            String workspaceKey, String projectKey, String issueKey, Long commentId) {
        return EntityReference.builder()
                .resourceType(ResourceType.ISSUE_COMMENT)
                .resourceId(commentId)
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .issueKey(issueKey)
                .build();
    }

    public static EntityReference forWorkspace(String workspaceKey) {
        return EntityReference.builder()
                .resourceType(ResourceType.WORKSPACE)
                .resourceId(null)
                .workspaceKey(workspaceKey)
                .build();
    }

    public static EntityReference forProject(String workspaceKey, String projectKey) {
        return EntityReference.builder()
                .resourceType(ResourceType.PROJECT)
                .resourceId(null)
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .build();
    }

    public static EntityReference forWorkspaceMember(String workspaceKey, Long memberId) {
        return EntityReference.builder()
                .resourceType(ResourceType.WORKSPACE_MEMBER)
                .resourceId(null)
                .memberId(memberId)
                .workspaceKey(workspaceKey)
                .build();
    }

    public static EntityReference forProjectMember(String workspaceKey, String projectKey, Long memberId) {
        return EntityReference.builder()
                .resourceType(ResourceType.PROJECT_MEMBER)
                .resourceId(null)
                .memberId(memberId)
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .build();
    }
}
