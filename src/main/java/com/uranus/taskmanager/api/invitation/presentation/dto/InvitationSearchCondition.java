package com.uranus.taskmanager.api.invitation.presentation.dto;

import java.util.List;

import com.uranus.taskmanager.api.invitation.domain.InvitationStatus;

public record InvitationSearchCondition(
	List<InvitationStatus> statuses
) {
	// 컴팩트 생성자를 사용해 기본 값(PENDING) 설정
	public InvitationSearchCondition {
		if (statuses == null || statuses.isEmpty()) {
			statuses = List.of(InvitationStatus.PENDING);
		}
	}

	public InvitationSearchCondition() {
		this(List.of(InvitationStatus.PENDING));
	}
}
