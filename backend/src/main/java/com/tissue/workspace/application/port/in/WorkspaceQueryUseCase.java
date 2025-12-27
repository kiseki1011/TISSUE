package com.tissue.workspace.application.port.in;

import static com.tissue.workspace.application.service.authorization.WorkspaceAuthExpressions.*;

import com.tissue.workspace.application.dto.out.query.WorkspaceDetail;
import org.springframework.security.access.prepost.PreAuthorize;

public interface WorkspaceQueryUseCase {

    @PreAuthorize(REQUIRES_WORKSPACE_MEMBER)
    WorkspaceDetail getDetail(String workspaceKey);

    // TODO: Workspace pagination api (오로지 참여 중인 것만 검색 가능)
    //  default
    //   - 20 workspaces
    //   - joinedDate DESC
    //  search by
    //   - createdDate (범위 검색 가능)
    //   - name
    //   - description (optional)
    //   - workspace key
    //  sort by
    //   - createdDate DESC
    //   - joinedDate DESC
    //   - total project numbers (optional)
    //   - total workspace members (optional)
}
