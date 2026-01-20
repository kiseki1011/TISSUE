package com.tissue.project.adapter.in.web.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.dto.request.AddProjectMembersCommand;
import com.tissue.project.domain.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AddProjectMembersRequest(List<MemberRequestConfig> members) {

    public record MemberRequestConfig(
            @NotNull Long memberId, @NotNull ProjectRole role) {}

    public AddProjectMembersCommand toCommand(ProjectMemberContext actorContext) {
        List<AddProjectMembersCommand.ProjectMemberConfig> configs = members.stream()
                .map(m -> new AddProjectMembersCommand.ProjectMemberConfig(m.memberId(), m.role()))
                .toList();

        return new AddProjectMembersCommand(configs, actorContext);
    }
}
