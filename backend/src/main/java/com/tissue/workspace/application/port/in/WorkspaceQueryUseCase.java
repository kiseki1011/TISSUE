package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.out.query.WorkspaceDetail;

public interface WorkspaceQueryUseCase {

    WorkspaceDetail getDetail(WorkspaceMemberContext actorContext);

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
