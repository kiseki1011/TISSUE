package com.tissue.feature.workspace.application.port.usecase;

import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberDetail;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberSearchResponse;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberSummary;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WorkspaceMemberQueryUseCase {

    List<WorkspaceMemberSearchResponse> searchMembers(
            String workspaceKey, @Nullable String projectKey, String query, Long actorMemberId);

    Page<WorkspaceMemberSummary> getWorkspaceMembers(
            String workspaceKey, @Nullable String keyword, Pageable pageable, Long actorMemberId);

    WorkspaceMemberDetail getWorkspaceMemberDetail(String workspaceKey, Long memberId, Long actorMemberId);
}
