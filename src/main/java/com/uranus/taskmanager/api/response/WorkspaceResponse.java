package com.uranus.taskmanager.api.response;

import com.uranus.taskmanager.api.domain.workspace.Workspace;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class WorkspaceResponse {

    private final String name;
    private final String description;
    private final String workspaceCode;
//    private final LocalDateTime createdAt;
//    private final LocalDateTime updatedAt;
//    private final LocalDateTime viewedAt;

    @Builder
    public WorkspaceResponse(String name, String description, String workspaceCode) {
        this.name = name;
        this.description = description;
        this.workspaceCode = workspaceCode;
    }

    public static WorkspaceResponse fromEntity(Workspace workspace) {
        return WorkspaceResponse.builder()
                .name(workspace.getName())
                .description(workspace.getDescription())
                .workspaceCode(workspace.getWorkspaceCode())
                .build();
    }

}
