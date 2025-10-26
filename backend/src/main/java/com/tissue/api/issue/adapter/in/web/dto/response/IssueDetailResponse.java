package com.tissue.api.issue.adapter.in.web.dto.response;

import java.time.Instant;

import com.tissue.api.issue.application.dto.response.IssueDetailDto;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.StateCategory;

import lombok.Builder;

@Builder
public record IssueDetailResponse(
	// Issue 기본 정보
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

	// IssueType
	IssueTypeInfo issueType,

	// WorkflowState
	StateInfo state,

	// Participants
	ParticipantInfo assignee,
	ParticipantInfo reporter,
	// ParticipantInfo creator,

	// Timestamps
	Instant createdAt,
	Instant updatedAt

	// TODO: 더 추가해야 하나?
	//  - progress
	//  - 현재 맺은 관계들(outgoing, ingoing)
	//  - parent 이슈
	//  - child 이슈들
	//  - subscribers는 보여줘야 하나? (따로 subscribers 조회 API 사용하도록 하는게 좋지 않을까?)
	//  - subscribers 수
	//  - reviewers
	//  - 예정 중
	//    - 추후에 sprint 기능 완성 후, 현재 속한 sprint
	//    - 추후에 파일 첨부 기능 염두
	//    - 추후에 tag 필드 염두
) {
	public record IssueTypeInfo(
		Long id,
		String displayName
		// String icon
	) {
	}

	public record StateInfo(
		Long id,
		String displayName,
		StateCategory category
	) {
	}

	public record ParticipantInfo(
		Long memberId,
		String username,
		String displayName
	) {
	}

	public static IssueDetailResponse from(IssueDetailDto dto) {
		return IssueDetailResponse.builder()
			.issueId(dto.issueId())
			.issueKey(dto.issueKey())
			.title(dto.title())
			.content(dto.content())
			.summary(dto.summary())
			.priority(dto.priority())
			.storyPoint(dto.storyPoint())
			.dueAt(dto.dueAt())
			.startedAt(dto.startedAt())
			.resolvedAt(dto.resolvedAt())
			.countBasedProgress(dto.countBasedProgress())
			.pointBasedProgress(dto.pointBasedProgress())
			.issueType(new IssueTypeInfo(dto.issueType().id(), dto.issueType().displayName()))
			.state(new StateInfo(dto.state().id(), dto.state().displayName(), dto.state().category()))
			.assignee(
				new ParticipantInfo(dto.assignee().memberId(), dto.assignee().username(), dto.assignee().displayName()))
			.reporter(
				new ParticipantInfo(dto.reporter().memberId(), dto.reporter().username(), dto.reporter().displayName()))
			// .creator(new ParticipantInfo(dto.creator().memberId(), dto.creator().username(), dto.creator().displayName()))
			.createdAt(dto.createdAt())
			.updatedAt(dto.updatedAt())
			.build();
	}
}
