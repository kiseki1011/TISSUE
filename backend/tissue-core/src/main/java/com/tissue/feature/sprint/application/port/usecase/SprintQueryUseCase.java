package com.tissue.feature.sprint.application.port.usecase;

import com.tissue.feature.sprint.application.dto.response.SprintDetail;
import com.tissue.feature.sprint.application.dto.response.SprintIssueKeys;

public interface SprintQueryUseCase {

    // TODO: getCurrentActiveSprint

    // TODO: getSprints - pagination api
    //  search
    //    - sprint status
    //    - total sprint issue numbers (by scope)
    //    - last completed (by scope)
    //    - created (by scope)
    //    - by creator
    //    - title, goal 검색
    //    - total days (by scope)

    SprintDetail getSprintDetail(String workspaceKey, Long sprintId, Long actorMemberId);

    SprintIssueKeys getSprintIssueKeys(String workspaceKey, Long sprintId, Long actorMemberId);
}
