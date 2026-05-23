package com.tissue.feature.workspace.application.service;

import com.tissue.feature.workspace.application.dto.response.query.DeletedWorkspaceSummary;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceDetail;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceSummaryResponse;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCount;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceQueryUseCase;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.exception.DuplicateWorkspaceKeyException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkspaceQueryService implements WorkspaceQueryUseCase {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

    @Override
    public WorkspaceDetail getDetail(String workspaceKey, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        return WorkspaceDetail.from(actor.getWorkspace());
    }

    @Override
    public List<WorkspaceSummaryResponse> getMyWorkspaces(Long actorMemberId) {
        List<WorkspaceMember> memberships =
                workspaceMemberQueryRepository.findAllWithWorkspaceByMemberId(actorMemberId);
        if (memberships.isEmpty()) {
            return List.of();
        }

        List<String> workspaceKeys =
                memberships.stream().map(WorkspaceMember::getWorkspaceKey).toList();
        Map<String, Long> memberCounts =
                workspaceMemberQueryRepository.countActiveByWorkspaceKeyIn(workspaceKeys).stream()
                        .collect(Collectors.toMap(
                                WorkspaceMemberCount::getWorkspaceKey, WorkspaceMemberCount::getCount));

        return memberships.stream()
                .map(wm -> WorkspaceSummaryResponse.from(wm, memberCounts.getOrDefault(wm.getWorkspaceKey(), 0L)))
                .toList();
    }

    @Override
    public List<DeletedWorkspaceSummary> getMyDeletedWorkspaces(Long actorMemberId) {
        List<Workspace> deletedWorkspaces = workspaceRepository.findDeletedWorkspacesByOwnerMemberId(actorMemberId);
        return deletedWorkspaces.stream().map(DeletedWorkspaceSummary::from).toList();
    }

    @Override
    public void checkKeyAvailability(String key) {
        if (workspaceRepository.existsByKey(key.toUpperCase())) {
            throw new DuplicateWorkspaceKeyException(key);
        }
    }
}
