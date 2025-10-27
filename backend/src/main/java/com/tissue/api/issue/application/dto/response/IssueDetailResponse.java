package com.tissue.api.issue.application.dto.response;

import java.time.Instant;

import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.StateCategory;

import lombok.Builder;

@Builder
public record IssueDetailResponse(
	Long issueId,
	String issueKey,

	String title,
	String content,
	String summary,
	IssuePriority priority,
	Integer storyPoint,
	Instant dueAt,
	Instant startedAt,
	Instant resolvedAt,
	Integer countBasedProgress,
	Integer pointBasedProgress,

	IssueTypeInfo issueType,

	StateInfo state,

	ParticipantInfo assignee,
	ParticipantInfo reporter,
	// ParticipantInfo creator,

	Instant createdAt,
	Instant updatedAt

	// TODO: 다음의 항목도 보여줘야 함
	//  - IssueType에 따른 storyPoint의 사용 가능 여부(만약 false라면 UI에서 storyPoint 필드 자체를 보여주지 않거나 비우기 위해서)
	//  - 현재 맺은 relations들(outgoing, ingoing)
	//  - parent 이슈
	//  - child 이슈들
	//  - creator
	//  - subscribers 수
	//  - (subscribers는 사용자가 조회를 따로 원할 때, 그렇기 때문에 따로 조회 API로 분리)
	//  - reviewers
	//  TODO(예정 중):
	//    - 추후에 sprint 기능 완성 후, 현재 속한 sprint
	//    - 추후에 파일 첨부 기능 염두
	//    - 추후에 tag 필드 염두
) {
	public record IssueTypeInfo(
		Long id,
		String displayName
		// ColorType color
		// String icon
	) {
	}

	public record StateInfo(
		Long id,
		String displayName,
		StateCategory category
		// ColorType color
		// String icon
	) {
	}

	public record ParticipantInfo(
		Long memberId,
		String username,
		String displayName
		// String profilePic? (예정 중)
	) {
	}
}
