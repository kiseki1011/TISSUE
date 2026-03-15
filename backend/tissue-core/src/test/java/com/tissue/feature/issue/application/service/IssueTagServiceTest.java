package com.tissue.feature.issue.application.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.tag.application.service.TagFinder;
import com.tissue.feature.tag.domain.Tag;
import com.tissue.shared.dto.IssueIdentifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssueTagServiceTest {

    @Mock
    private IssueFinder issueFinder;

    @Mock
    private ProjectMemberFinder projectMemberFinder;

    @Mock
    private TagFinder tagFinder;

    @InjectMocks
    private IssueTagService sut;

    @Test
    @DisplayName("success: add issue tag")
    void successAddIssueTag() {
        // given
        Long actorMemberId = 1L;
        IssueIdentifier iid = new IssueIdentifier("WORKSPACE", "PROJ", "PROJ-123");
        Long tagId = 1L;

        ProjectMember actor = mock(ProjectMember.class);
        Issue issue = mock(Issue.class);
        Tag tag = mock(Tag.class);

        given(projectMemberFinder.getWithWorkspaceMember(iid.workspaceKey(), iid.projectKey(), actorMemberId))
                .willReturn(actor);
        given(issueFinder.getWithProjectBy(iid.workspaceKey(), iid.issueKey())).willReturn(issue);
        given(tagFinder.getWithProjectBy(iid.workspaceKey(), iid.projectKey(), tagId))
                .willReturn(tag);

        // when
        sut.addTag(iid, tagId, actorMemberId);

        // then
        then(issue).should().addTag(tag);
    }

    @Test
    @DisplayName("success: remove issue tag")
    void successRemoveIssueTag() {
        // given
        Long actorMemberId = 1L;
        IssueIdentifier iid = new IssueIdentifier("WORKSPACE", "PROJ", "PROJ-123");
        Long tagId = 1L;

        ProjectMember actor = mock(ProjectMember.class);
        Issue issue = mock(Issue.class);
        Tag tag = mock(Tag.class);

        given(projectMemberFinder.getWithWorkspaceMember(iid.workspaceKey(), iid.projectKey(), actorMemberId))
                .willReturn(actor);
        given(issueFinder.getWithProjectBy(iid.workspaceKey(), iid.issueKey())).willReturn(issue);
        given(tagFinder.getWithProjectBy(iid.workspaceKey(), iid.projectKey(), tagId))
                .willReturn(tag);

        // when
        sut.removeTag(iid, tagId, actorMemberId);

        // then
        then(issue).should().removeTag(tag);
    }
}
