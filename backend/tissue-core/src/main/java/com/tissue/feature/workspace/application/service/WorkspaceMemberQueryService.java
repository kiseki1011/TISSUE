package com.tissue.feature.workspace.application.service;

import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberDetail;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberSearchResponse;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberSummary;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceMemberQueryUseCase;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.exception.WorkspaceMemberNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Override
    public Page<WorkspaceMemberSummary> getWorkspaceMembers(
            String workspaceKey, @Nullable String keyword, Pageable pageable, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        Page<WorkspaceMember> page = (keyword == null || keyword.isBlank())
                ? workspaceMemberQueryRepository.pageActiveByWorkspaceKey(workspaceKey, pageable)
                : workspaceMemberQueryRepository.pageActiveByWorkspaceKeyAndKeyword(workspaceKey, keyword, pageable);

        return page.map(WorkspaceMemberSummary::from);
    }

    @Override
    public WorkspaceMemberDetail getWorkspaceMemberDetail(String workspaceKey, Long memberId, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        WorkspaceMember workspaceMember = workspaceMemberQueryRepository
                .findWithMemberByWorkspaceKeyAndMemberId(workspaceKey, memberId)
                .orElseThrow(() -> new WorkspaceMemberNotFoundException(workspaceKey, memberId));

        return WorkspaceMemberDetail.from(workspaceMember);
    }
}
