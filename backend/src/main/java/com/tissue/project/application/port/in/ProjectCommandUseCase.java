package com.tissue.project.application.port.in;

import static com.tissue.project.application.service.authorization.ProjectAuthExpressions.*;
import static com.tissue.workspace.application.service.authorization.WorkspaceAuthExpressions.*;

import com.tissue.project.application.dto.request.CreateProjectCommand;
import com.tissue.project.application.dto.request.DeleteProjectCommand;
import com.tissue.project.application.dto.request.UpdateProjectCommand;
import com.tissue.project.application.dto.response.ProjectCommandResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ProjectCommandUseCase {

    @PreAuthorize(REQUIRES_WORKSPACE_MEMBER)
    ProjectCommandResult create(CreateProjectCommand cmd);

    @PreAuthorize(REQUIRES_PROJECT_ADMIN)
    ProjectCommandResult update(UpdateProjectCommand cmd);

    @PreAuthorize(REQUIRES_WORKSPACE_ADMIN)
    ProjectCommandResult delete(DeleteProjectCommand cmd);

    // TODO: archive()
    // TODO: migrateProjectKey()
    //  - 선택사항. 여유 있으면 구현하기.
    //  - softDelete 상태에서도 가능하도록?
}
