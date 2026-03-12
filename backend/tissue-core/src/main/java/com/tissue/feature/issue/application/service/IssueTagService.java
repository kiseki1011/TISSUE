package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.port.usecase.IssueTagUseCase;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.tag.application.service.TagFinder;
import com.tissue.feature.tag.domain.Tag;
import com.tissue.shared.dto.IssueIdentifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueTagService implements IssueTagUseCase {

    private final IssueFinder issueFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final TagFinder tagFinder;

    @Override
    public void addTag(IssueIdentifier iid, Long tagId, Long actorMemberId) {
        projectMemberFinder.getWithWorkspaceMember(iid.workspaceKey(), iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectBy(iid.workspaceKey(), iid.issueKey());
        Tag tag = tagFinder.getWithProjectBy(iid.workspaceKey(), iid.projectKey(), tagId);

        issue.addTag(tag);
    }

    @Override
    public void removeTag(IssueIdentifier iid, Long tagId, Long actorMemberId) {
        projectMemberFinder.getWithWorkspaceMember(iid.workspaceKey(), iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectBy(iid.workspaceKey(), iid.issueKey());
        Tag tag = tagFinder.getWithProjectBy(iid.workspaceKey(), iid.projectKey(), tagId);

        issue.removeTag(tag);
    }
}
