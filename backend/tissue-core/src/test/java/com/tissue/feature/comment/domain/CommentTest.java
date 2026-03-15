package com.tissue.feature.comment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CommentTest {

    @Nested
    @DisplayName("create comment")
    class Create {

        @Test
        @DisplayName("success: create comment without parent")
        void successCreateComment() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Member member = TestFixtures.member("author");
            WorkspaceMember author = TestFixtures.workspaceMember(member, ws, WorkspaceRole.MEMBER);
            Issue issue = TestFixtures.issue(project, "issue title", IssueHierarchy.STANDARD);

            // when
            Comment comment = Comment.create(author, issue, "hello world", null);

            // then
            assertThat(comment.getContent()).isEqualTo("hello world");
            assertThat(comment.getAuthor()).isEqualTo(author);
            assertThat(comment.getIssue()).isEqualTo(issue);
            assertThat(comment.getWorkspaceKey()).isEqualTo("WORKSPACE");
            assertThat(comment.isEdited()).isFalse();
            assertThat(comment.getParentComment()).isNull();
        }

        @Test
        @DisplayName("success: create reply comment with parent")
        void successCreateReplyComment() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Member member = TestFixtures.member("author");
            WorkspaceMember author = TestFixtures.workspaceMember(member, ws, WorkspaceRole.MEMBER);
            Issue issue = TestFixtures.issue(project, "issue title", IssueHierarchy.STANDARD);
            Comment parent = Comment.create(author, issue, "parent comment", null);

            // when
            Comment reply = Comment.create(author, issue, "reply comment", parent);

            // then
            assertThat(reply.getParentComment()).isEqualTo(parent);
            assertThat(parent.getChildComments()).containsExactly(reply);
        }

        @Test
        @DisplayName("fail: throws ProjectArchivedException when project is archived")
        void failCreate_If_ProjectArchived() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project archivedProject = TestFixtures.archivedProject(ws, "PROJ");
            Member member = TestFixtures.member("author");
            WorkspaceMember author = TestFixtures.workspaceMember(member, ws, WorkspaceRole.MEMBER);

            // when & then
            assertThatThrownBy(() -> Comment.create(
                            author, TestFixtures.issue(archivedProject, "t", IssueHierarchy.STANDARD), "content", null))
                    .isInstanceOf(ProjectArchivedException.class);
        }

        @Test
        @DisplayName("fail: throws BadRequestException when parent comment belongs to different issue")
        void failCreate_If_ParentBelongsToDifferentIssue() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Member member = TestFixtures.member("author");
            WorkspaceMember author = TestFixtures.workspaceMember(member, ws, WorkspaceRole.MEMBER);
            Issue issue1 = TestFixtures.issue(project, "issue 1", IssueHierarchy.STANDARD);
            Issue issue2 = TestFixtures.issue(project, "issue 2", IssueHierarchy.STANDARD);
            Comment parentOnIssue1 = Comment.create(author, issue1, "parent", null);

            // when & then
            assertThatThrownBy(() -> Comment.create(author, issue2, "reply", parentOnIssue1))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("fail: throws BadRequestException when nesting exceeds 1 depth")
        void failCreate_If_NestingExceedsLimit() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Member member = TestFixtures.member("author");
            WorkspaceMember author = TestFixtures.workspaceMember(member, ws, WorkspaceRole.MEMBER);
            Issue issue = TestFixtures.issue(project, "issue title", IssueHierarchy.STANDARD);
            Comment root = Comment.create(author, issue, "root", null);
            Comment reply = Comment.create(author, issue, "reply", root);

            // when & then
            assertThatThrownBy(() -> Comment.create(author, issue, "nested reply", reply))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
