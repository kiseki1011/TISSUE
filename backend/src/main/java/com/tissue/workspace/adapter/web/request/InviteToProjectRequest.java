package com.tissue.workspace.adapter.web.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.workspace.application.dto.request.InviteToProjectCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record InviteToProjectRequest(
    @NotEmpty Set<@Email @NotBlank String> emails,
    @NotNull ProjectRole role) {

    public InviteToProjectCommand toCommand(ProjectMemberContext actorContext) {
        return new InviteToProjectCommand(emails, role, actorContext);
    }
}
