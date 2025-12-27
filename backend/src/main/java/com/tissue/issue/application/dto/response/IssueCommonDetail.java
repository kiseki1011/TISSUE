package com.tissue.issue.application.dto.response;

import com.tissue.issue.application.dto.response.info.IssueTypeInfo;
import com.tissue.issue.application.dto.response.info.ParticipantInfo;
import com.tissue.issue.application.dto.response.info.StateInfo;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueReviewer;
import com.tissue.issue.domain.enums.IssuePriority;
import com.tissue.project.domain.ProjectMember;
import java.time.Instant;
import java.util.List;
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

        // TODO: 진행도는 따로 분리해서 조회하는거 고려(캐싱 고려)
        Integer countBasedProgress,
        Integer pointBasedProgress,
        IssueTypeInfo issueType,
        StateInfo currentState,

        // TODO: 관련자 따로 분리해서 조회하는 것 고려
        ParticipantInfo author,
        ParticipantInfo assignee,
        ParticipantInfo reporter,
        List<ParticipantInfo> reviewers,
        ParticipantInfo lastUpdatedBy,
        Integer subscribersCount,
        Instant createdAt,
        Instant lastUpdatedAt) {

    public static IssueCommonDetail from(
            Issue issue,
            ProjectMember author,
            ProjectMember updatedBy,
            List<IssueReviewer> reviewers) {
        return IssueCommonDetail.builder()
                .issueId(issue.getId())
                .issueKey(issue.getKey())
                .title(issue.getTitle())
                .content(issue.getContent())
                .summary(issue.getSummary())
                .priority(issue.getPriority())
                .storyPoint(issue.getStoryPoint())
                .dueAt(issue.getSchedule().getDueAt())
                .startedAt(issue.getSchedule().getStartedAt())
                .resolvedAt(issue.getSchedule().getResolvedAt())
                .countBasedProgress(issue.getProgress().getCountBasedProgress())
                .pointBasedProgress(issue.getProgress().getPointBasedProgress())
                .issueType(IssueTypeInfo.from(issue.getIssueType()))
                .currentState(StateInfo.from(issue.getCurrentState()))
                .author(ParticipantInfo.from(author))
                .assignee(ParticipantInfo.from(issue.getParticipants().getAssignee()))
                .reporter(ParticipantInfo.from(issue.getParticipants().getReporter()))
                .lastUpdatedBy(ParticipantInfo.from(updatedBy))
                .reviewers(
                        reviewers.stream()
                                .map(IssueReviewer::getReviewer)
                                .map(ParticipantInfo::from)
                                .toList())
                .subscribersCount(issue.getSubscribersCount())
                .createdAt(issue.getCreatedAt())
                .lastUpdatedAt(issue.getLastModifiedAt())
                .build();
    }
}
