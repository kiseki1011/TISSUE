package com.tissue.issue.application.port.in;

import com.tissue.project.application.dto.ProjectMemberContext;

public interface IssueParticipantUseCase {

    void assign(String issueKey, Long targetMemberId, ProjectMemberContext actorContext);

    void unassign(String issueKey, ProjectMemberContext actorContext);

    void subscribe(String issueKey, ProjectMemberContext actorContext);

    void unsubscribe(String issueKey, ProjectMemberContext actorContext);

    void addReviewer(String issueKey, Long targetMemberId, ProjectMemberContext actorContext);

    void removeReviewer(String issueKey, Long targetMemberId, ProjectMemberContext actorContext);
}
