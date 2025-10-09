package com.tissue.api.workflow.domain.gaurd;

import java.util.Map;

import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.workflow.domain.model.WorkflowTransition;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuardContext {
	private final Issue issue; // 전이 대상 이슈
	private final WorkflowTransition transition; // 실행하려는 전이
	private final Long actorMemberId; // 전이를 실행하는 멤버 ID
	private final String workspaceKey; // 전이가 일어나는 워크스페이스 키
	private final Map<String, Object> params; // Guard별 파라미터 (JSON에서 파싱)
}
