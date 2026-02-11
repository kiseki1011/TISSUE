package com.tissue.feature.sprint.application.port.usecase;

import com.tissue.feature.project.application.dto.ProjectMemberContext;
import com.tissue.feature.sprint.application.dto.response.SprintDetail;
import com.tissue.feature.sprint.application.dto.response.SprintIssueKeys;

public interface SprintQueryUseCase {

    // TODO: getCurrentActiveSprint
    // TODO: getSprints - pagination api
    //  - sprint status
    //  - total sprint issue numbers?
    //  - last completed
    //  - created
    //  - by creator?
    //  - 해당 스프린트 관련자(해당 이슈들의 관련자)에 따라 검색 가능?
    //  - title, goal 검색
    //  - 총 소요 기간

    SprintDetail getSprintDetail(Long sprintId, ProjectMemberContext actorContext);

    SprintIssueKeys getSprintIssueKeys(Long sprintId, ProjectMemberContext actorContext);
}
