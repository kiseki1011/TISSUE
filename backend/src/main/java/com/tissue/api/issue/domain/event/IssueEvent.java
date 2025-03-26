package com.tissue.api.issue.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.issue.domain.enums.IssueType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Todo
 *  - Issue를 전달하는게 아니라 DTO를 만들어서 필요한 데이터만 전달하도록 리팩토링
 *  - 또는 issue id만 전달하고, 핸들러에서 id로 다시 조회하는 방식
 */
@Getter
@RequiredArgsConstructor
public abstract class IssueEvent implements DomainEvent {

	private final UUID eventId = UUID.randomUUID();
	private final LocalDateTime occurredAt = LocalDateTime.now();

	private final Long issueId;
	private final String issueKey;
	private final String workspaceCode;
	private final IssueType issueType;
	private final Long triggeredByWorkspaceMemberId;

	@Override
	public String getType() {
		return this.getClass().getSimpleName();
	}
}
