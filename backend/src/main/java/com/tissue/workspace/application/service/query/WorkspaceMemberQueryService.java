package com.tissue.workspace.application.service.query;

import com.tissue.workspace.adapter.in.web.dto.response.WorkspaceMemberSearchResponse;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.port.in.WorkspaceMemberQueryUseCase;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.workspace.domain.WorkspaceMember;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkspaceMemberQueryService implements WorkspaceMemberQueryUseCase {

    private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    @Override
    public List<WorkspaceMemberSearchResponse> searchMembers(
            WorkspaceMemberContext context, String query, @Nullable String projectKey) {

        workspaceAuthorizationService.requireWorkspaceMember(context);

        List<WorkspaceMember> members;

        if (projectKey != null) {
            members = workspaceMemberQueryRepository.searchProjectMembers(context.workspaceKey(), projectKey, query);
        } else {
            members = workspaceMemberQueryRepository.searchMembers(context.workspaceKey(), query);
        }

        return members.stream().map(WorkspaceMemberSearchResponse::from).toList();
    }
}
