package com.tissue.feature.comment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.comment.application.dto.request.CreateCommentCommand;
import com.tissue.feature.comment.application.dto.request.UpdateCommentCommand;
import com.tissue.feature.comment.application.dto.response.CommentCreateResponse;
import com.tissue.feature.comment.application.port.repository.CommentRepository;
import com.tissue.feature.comment.domain.Comment;
import com.tissue.feature.comment.domain.exception.CommentNotFoundException;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssueCommentCommandServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private IssueFinder issueFinder;

    @Mock
    private WorkspaceMemberFinder workspaceMemberFinder;

    @Mock
    private CommentAuthorizationService commentAuthorizationService;

    @Mock
    private CommentEventPublisher eventPublisher;

    @InjectMocks
    private IssueCommentCommandService sut;

    @Nested
    @DisplayName("create comment")
    class CreateComment {

        @Test
        @DisplayName("success: create comment without parent")
        void successCreateComment() {
            // given
            Long memberId = 1L;
            IssueIdentifier iid = IssueIdentifier.of("WORKSPACE", "PROJ", "PROJ-1");

            WorkspaceMember author = mock(WorkspaceMember.class);
            Issue issue = mock(Issue.class);
            Project project = mock(Project.class);

            CreateCommentCommand cmd = CreateCommentCommand.builder()
                    .content("comment content")
                    .mentionedUsernames(List.of())
                    .parentCommentId(null)
                    .build();

            given(issue.getWorkspaceKey()).willReturn("WORKSPACE");
            given(issue.getKey()).willReturn("PROJ-1");
            given(issue.getProject()).willReturn(project);
            given(project.isArchived()).willReturn(false);
            given(workspaceMemberFinder.getWithWorkspace(iid.workspaceKey(), memberId))
                    .willReturn(author);
            given(issueFinder.getWithProjectBy(iid.workspaceKey(), iid.issueKey()))
                    .willReturn(issue);

            // when
            CommentCreateResponse response = sut.create(iid, cmd, memberId);

            // then
            then(commentRepository).should().save(any(Comment.class));
            then(eventPublisher).should().publishCommentAdded(eq(issue), any(Comment.class), eq(List.of()), eq(author));
            assertThat(response.issueKey()).isEqualTo(iid.issueKey());
        }

        @Test
        @DisplayName("fail: throws CommentNotFoundException when parent comment does not exist")
        void failCreateReply_If_ParentCommentNotFound() {
            // given
            Long memberId = 1L;
            Long parentCommentId = 999L;
            IssueIdentifier iid = IssueIdentifier.of("WORKSPACE", "PROJ", "PROJ-1");

            WorkspaceMember author = mock(WorkspaceMember.class);
            Issue issue = mock(Issue.class);

            CreateCommentCommand cmd = CreateCommentCommand.builder()
                    .content("reply content")
                    .mentionedUsernames(List.of())
                    .parentCommentId(parentCommentId)
                    .build();

            given(workspaceMemberFinder.getWithWorkspace(iid.workspaceKey(), memberId))
                    .willReturn(author);
            given(issueFinder.getWithProjectBy(iid.workspaceKey(), iid.issueKey()))
                    .willReturn(issue);
            given(commentRepository.findByIssueAndId(issue, parentCommentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sut.create(iid, cmd, memberId)).isInstanceOf(CommentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update comment")
    class UpdateComment {

        @Test
        @DisplayName("success: update comment content")
        void successUpdateComment() {
            // given
            Long memberId = 1L;
            Long commentId = 10L;
            IssueIdentifier iid = IssueIdentifier.of("WORKSPACE", "PROJ", "PROJ-1");

            WorkspaceMember actor = mock(WorkspaceMember.class);
            Comment comment = mock(Comment.class);
            Issue issue = mock(Issue.class);

            UpdateCommentCommand cmd = new UpdateCommentCommand("updated content", List.of("user1"));

            given(workspaceMemberFinder.getWithWorkspace(iid.workspaceKey(), memberId))
                    .willReturn(actor);
            given(commentRepository.findWithProjectAndIssueByKeysAndId(iid.workspaceKey(), iid.issueKey(), commentId))
                    .willReturn(Optional.of(comment));
            given(comment.getIssue()).willReturn(issue);

            // when
            sut.update(iid, commentId, cmd, memberId);

            // then
            then(commentAuthorizationService).should().requireCommentEditPermission(comment, actor);
            then(comment).should().updateContent("updated content");
            then(eventPublisher).should().publishCommentUpdated(issue, comment, List.of("user1"), actor);
        }
    }

    @Nested
    @DisplayName("delete comment")
    class DeleteComment {

        @Test
        @DisplayName("success: soft delete comment")
        void successDeleteComment() {
            // given
            Long memberId = 1L;
            Long commentId = 10L;
            IssueIdentifier iid = IssueIdentifier.of("WORKSPACE", "PROJ", "PROJ-1");

            WorkspaceMember actor = mock(WorkspaceMember.class);
            Comment comment = mock(Comment.class);
            Issue issue = mock(Issue.class);

            given(workspaceMemberFinder.getWithWorkspace(iid.workspaceKey(), memberId))
                    .willReturn(actor);
            given(commentRepository.findWithProjectAndIssueByKeysAndId(iid.workspaceKey(), iid.issueKey(), commentId))
                    .willReturn(Optional.of(comment));
            given(comment.getIssue()).willReturn(issue);

            // when
            sut.delete(iid, commentId, memberId);

            // then
            then(commentAuthorizationService).should().requireCommentEditPermission(comment, actor);
            then(comment).should().softDelete();
            then(eventPublisher).should().publishCommentDeleted(issue, comment, actor);
        }
    }
}
