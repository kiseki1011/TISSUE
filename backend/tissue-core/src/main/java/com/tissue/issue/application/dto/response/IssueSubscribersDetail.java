package com.tissue.issue.application.dto.response;

import com.tissue.issue.application.dto.response.info.ParticipantInfo;
import com.tissue.issue.domain.IssueSubscriber;
import java.util.List;

public record IssueSubscribersDetail(List<ParticipantInfo> subscribers, int totalCount) {

    public static IssueSubscribersDetail from(List<IssueSubscriber> subscribers) {
        return new IssueSubscribersDetail(
                subscribers.stream()
                        .map(IssueSubscriber::getSubscriber)
                        .map(ParticipantInfo::from)
                        .toList(),
                subscribers.size());
    }
}
