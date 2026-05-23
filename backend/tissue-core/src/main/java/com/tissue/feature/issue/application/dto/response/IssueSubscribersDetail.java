package com.tissue.feature.issue.application.dto.response;

import com.tissue.feature.issue.application.dto.response.info.ProjectMemberInfo;
import com.tissue.feature.issue.domain.IssueSubscriber;
import java.util.List;

public record IssueSubscribersDetail(List<ProjectMemberInfo> subscribers, int totalCount) {
    public static IssueSubscribersDetail from(List<IssueSubscriber> subscribers) {
        return new IssueSubscribersDetail(
                subscribers.stream()
                        .map(IssueSubscriber::getSubscriber)
                        .map(ProjectMemberInfo::from)
                        .toList(),
                subscribers.size());
    }
}
