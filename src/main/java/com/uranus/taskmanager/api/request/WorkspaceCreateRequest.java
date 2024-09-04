package com.uranus.taskmanager.api.request;

import com.uranus.taskmanager.api.domain.workspace.Workspace;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
@Builder
public class WorkspaceCreateRequest {

    @Size(min = 2, max = 50, message = "Workspace name must be 2 ~ 50 characters long")
    @NotBlank(message = "Workspace name must not be blank")
    private final String name;

    @Size(min = 1, max = 255, message = "Workspace name must be 1 ~ 255 characters long")
    @NotBlank(message = "Workspace description must not be blank")
    private final String description;

    public Workspace toEntity() {
        return Workspace.builder()
                .name(name)
                .description(description)
                .build();
    }

}
