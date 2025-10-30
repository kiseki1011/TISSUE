package com.tissue.api.issue.application.dto.response;

import java.time.Instant;
import java.util.List;

import com.tissue.api.issue.application.dto.response.info.IssueTypeInfo;
import com.tissue.api.issue.application.dto.response.info.ParticipantInfo;
import com.tissue.api.issue.application.dto.response.info.StateInfo;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueReviewer;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.Builder;

@Builder
public record IssueCommonDetail(
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

	ParticipantInfo author,
	ParticipantInfo assignee,
	ParticipantInfo reporter,
	List<ParticipantInfo> reviewers,
	ParticipantInfo lastUpdatedBy,

	Integer subscribersCount,

	Instant createdAt,
	Instant lastUpdatedAt

	//  TODO(예정 중):
	//   - 추후에 sprint 기능 완성 후, 현재 속한 sprint
	//   - 추후에 파일 첨부 기능 염두
	//   - 추후에 tag 필드 염두
) {
	public static IssueCommonDetail from(Issue issue, WorkspaceMember author, WorkspaceMember updatedBy,
		List<IssueReviewer> reviewers) {
		return IssueCommonDetail.builder()
			.issueId(issue.getId())
			.issueKey(issue.getKey())
			.title(issue.getTitle())
			.content(issue.getContent().getContent())
			.summary(issue.getContent().getSummary())
			.priority(issue.getPriority())
			.storyPoint(issue.getStoryPoint())
			.dueAt(issue.getSchedule().getDueAt())
			.startedAt(issue.getSchedule().getStartedAt())
			.resolvedAt(issue.getSchedule().getResolvedAt())
			.countBasedProgress(issue.getProgress().getCountBasedProgress())
			.pointBasedProgress(issue.getProgress().getPointBasedProgress())

			.issueType(IssueTypeInfo.from(issue.getIssueType()))
			.state(StateInfo.from(issue.getCurrentState()))

			.author(ParticipantInfo.from(author))
			.assignee(ParticipantInfo.from(issue.getParticipants().getAssignee()))
			.reporter(ParticipantInfo.from(issue.getParticipants().getReporter()))
			.lastUpdatedBy(ParticipantInfo.from(updatedBy))

			.reviewers(reviewers.stream()
				.map(IssueReviewer::getReviewer)
				.map(ParticipantInfo::from)
				.toList())

			.subscribersCount(issue.getSubscribersCount())

			.createdAt(issue.getCreatedAt())
			.lastUpdatedAt(issue.getLastModifiedAt())
			.build();
	}
}
