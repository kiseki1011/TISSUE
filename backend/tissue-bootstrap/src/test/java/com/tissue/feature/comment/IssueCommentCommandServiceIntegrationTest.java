package com.tissue.feature.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.comment.application.dto.request.CreateCommentCommand;
import com.tissue.feature.comment.application.dto.response.CommentCreateResponse;
import com.tissue.feature.comment.application.port.repository.CommentRepository;
import com.tissue.feature.comment.application.service.IssueCommentCommandService;
import com.tissue.feature.comment.domain.Comment;
import com.tissue.feature.comment.domain.exception.CommentErrorCode;
import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.dto.response.IssueCreateResponse;
import com.tissue.feature.issue.application.service.IssueLifecycleService;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class IssueCommentCommandServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private IssueCommentCommandService issueCommentCommandService;

    @Autowired
    private IssueLifecycleService issueLifecycleService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private IssueTypeRepository issueTypeRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberCommandRepository workspaceMemberRepository;

    @Autowired
    private ProjectCommandRepository projectRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberRepository;

    private static final ProjectIdentifier PID = new ProjectIdentifier("WORKSPACE", "PROJ");

    private Member member;
    private Long issueTypeId;
    private String issueKey;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create("test@tissue.com", "testuser", "HongGilDong"));
        Workspace workspace = workspaceRepository.save(Workspace.create(PID.workspaceKey(), "Test Workspace", null));
        Project project = projectRepository.save(Project.create(workspace, PID.projectKey(), "Test Project", null));
        WorkspaceMember workspaceMember =
                workspaceMemberRepository.save(WorkspaceMember.create(member, workspace, WorkspaceRole.OWNER));
        projectMemberRepository.save(ProjectMember.createManager(project, workspaceMember));

        Workflow workflow = Workflow.create(project, Name.of("Test Workflow"), null, ColorType.BRIGHT_CYAN);
        workflow.addState(Name.of("Open"), null, ColorType.GREEN, StateCategory.INITIAL);
        workflow.addState(Name.of("Done"), null, ColorType.BLACK, StateCategory.COMPLETED);
        workflowRepository.save(workflow);

        IssueType issueType = IssueType.create(
                project,
                Name.of("Story"),
                null,
                ColorType.RED,
                IconType.CIRCLE_FILLED,
                IssueHierarchy.STANDARD,
                workflow);
        issueTypeRepository.save(issueType);
        issueTypeId = issueType.getId();

        em.flush();
        em.clear();

        issueKey = createBasicIssue();
    }

    @Nested
    @DisplayName("create comment")
    class CreateComment {

        @Test
        @DisplayName("creates root comment on issue")
        void successCreateRootComment() {
            // given
            IssueIdentifier iid = new IssueIdentifier(PID.workspaceKey(), PID.projectKey(), issueKey);
            CreateCommentCommand cmd = CreateCommentCommand.builder()
                    .content("First comment")
                    .mentionedUsernames(List.of())
                    .parentCommentId(null)
                    .build();

            // when
            CommentCreateResponse response = issueCommentCommandService.create(iid, cmd, member.getId());
            em.flush();
            em.clear();

            // then
            Comment comment = commentRepository
                    .findWithProjectAndIssueByKeysAndId(PID.workspaceKey(), issueKey, response.commentId())
                    .orElseThrow();

            assertThat(comment.getContent()).isEqualTo("First comment");
            assertThat(comment.getParentComment()).isNull();
            assertThat(comment.isEdited()).isFalse();
        }

        @Test
        @DisplayName("creates reply to root comment")
        void successCreateReply() {
            // given
            IssueIdentifier iid = new IssueIdentifier(PID.workspaceKey(), PID.projectKey(), issueKey);
            CommentCreateResponse root = issueCommentCommandService.create(
                    iid,
                    CreateCommentCommand.builder()
                            .content("Root comment")
                            .mentionedUsernames(List.of())
                            .build(),
                    member.getId());
            em.flush();
            em.clear();

            CreateCommentCommand replyCmd = CreateCommentCommand.builder()
                    .content("Reply comment")
                    .mentionedUsernames(List.of())
                    .parentCommentId(root.commentId())
                    .build();

            // when
            CommentCreateResponse replyResponse = issueCommentCommandService.create(iid, replyCmd, member.getId());
            em.flush();
            em.clear();

            // then
            Comment reply = commentRepository
                    .findWithProjectAndIssueByKeysAndId(PID.workspaceKey(), issueKey, replyResponse.commentId())
                    .orElseThrow();

            assertThat(reply.getContent()).isEqualTo("Reply comment");
            assertThat(reply.getParentComment()).isNotNull();
            assertThat(reply.getParentComment().getId()).isEqualTo(root.commentId());
        }

        @Test
        @DisplayName("fails to create nested reply (the depth cannot exceed 1)")
        void failIfNestedReplyExceedsDepthLimit() {
            // given
            IssueIdentifier iid = new IssueIdentifier(PID.workspaceKey(), PID.projectKey(), issueKey);

            CommentCreateResponse root = issueCommentCommandService.create(
                    iid,
                    CreateCommentCommand.builder()
                            .content("Root")
                            .mentionedUsernames(List.of())
                            .build(),
                    member.getId());

            CommentCreateResponse reply = issueCommentCommandService.create(
                    iid,
                    CreateCommentCommand.builder()
                            .content("Reply")
                            .mentionedUsernames(List.of())
                            .parentCommentId(root.commentId())
                            .build(),
                    member.getId());

            em.flush();
            em.clear();

            CreateCommentCommand nestedReplyCmd = CreateCommentCommand.builder()
                    .content("Nested reply")
                    .mentionedUsernames(List.of())
                    .parentCommentId(reply.commentId())
                    .build();

            // when & then
            assertThatThrownBy(() -> issueCommentCommandService.create(iid, nestedReplyCmd, member.getId()))
                    .isInstanceOf(ResourceConflictException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommentErrorCode.NESTED_COMMENT_LIMIT_EXCEEDED);
        }
    }

    private String createBasicIssue() {
        CreateIssueCommand cmd = CreateIssueCommand.builder()
                .title("Test Issue")
                .priority(IssuePriority.P2)
                .issueTypeId(issueTypeId)
                .customFields(Map.of())
                .build();

        IssueCreateResponse response = issueLifecycleService.create(PID, cmd, member.getId());
        em.flush();
        em.clear();
        return response.issueKey();
    }
}
