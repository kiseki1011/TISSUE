package com.tissue.feature.workspace.application.service;

import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberSearchResponse;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceMemberQueryUseCase;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkspaceMemberQueryService implements WorkspaceMemberQueryUseCase {

    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

    @Override
    public List<WorkspaceMemberSearchResponse> searchMembers(
            String workspaceKey, @Nullable String projectKey, String query, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        List<WorkspaceMember> members;

        if (projectKey != null) {
            members = workspaceMemberQueryRepository.searchProjectMembers(workspaceKey, projectKey, query);
        } else {
            members = workspaceMemberQueryRepository.searchMembers(workspaceKey, query);
        }

        return members.stream().map(WorkspaceMemberSearchResponse::from).toList();
    }
}
