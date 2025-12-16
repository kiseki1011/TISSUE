package com.tissue.sprint.application.port.in;

import org.springframework.transaction.annotation.Transactional;

import com.tissue.sprint.application.dto.request.GetSprintDetailQuery;
import com.tissue.sprint.application.dto.request.GetSprintIssueKeysQuery;
import com.tissue.sprint.application.dto.response.SprintDetail;
import com.tissue.sprint.application.dto.response.SprintIssueKeys;

@Transactional(readOnly = true)
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

	SprintDetail getSprintDetail(GetSprintDetailQuery query);

	SprintIssueKeys getSprintIssueKeys(GetSprintIssueKeysQuery query);
}
