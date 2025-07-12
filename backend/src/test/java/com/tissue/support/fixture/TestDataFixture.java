package com.tissue.support.fixture;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.comment.domain.model.Comment;
import com.tissue.api.comment.domain.model.IssueComment;
import com.tissue.api.comment.domain.model.ReviewComment;
import com.tissue.api.comment.infrastructure.repository.CommentRepository;
import com.tissue.api.invitation.domain.enums.InvitationStatus;
import com.tissue.api.invitation.domain.model.Invitation;
import com.tissue.api.invitation.infrastructure.repository.InvitationRepository;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.IssueAssignee;
import com.tissue.api.issue.domain.model.IssueReviewer;
import com.tissue.api.issue.domain.model.enums.BugSeverity;
import com.tissue.api.issue.domain.model.enums.IssuePriority;
import com.tissue.api.issue.domain.model.enums.IssueType;
import com.tissue.api.issue.domain.model.types.Bug;
import com.tissue.api.issue.domain.model.types.Epic;
import com.tissue.api.issue.domain.model.types.Story;
import com.tissue.api.issue.domain.model.types.SubTask;
import com.tissue.api.issue.domain.model.types.Task;
import com.tissue.api.issue.infrastructure.repository.IssueAssigneeRepository;
import com.tissue.api.issue.infrastructure.repository.IssueRepository;
import com.tissue.api.issue.infrastructure.repository.IssueReviewerRepository;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.infrastructure.repository.MemberRepository;
import com.tissue.api.review.domain.model.Review;
import com.tissue.api.review.domain.model.enums.ReviewStatus;
import com.tissue.api.review.infrastructure.repository.ReviewRepository;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspace.infrastructure.repository.WorkspaceRepository;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Component
@Transactional
@RequiredArgsConstructor
public class TestDataFixture {

	@Autowired
	private final IssueRepository issueRepository;
	@Autowired
	private final ReviewRepository reviewRepository;
	@Autowired
	private final IssueReviewerRepository issueReviewerRepository;
	@Autowired
	private final CommentRepository commentRepository;
	@Autowired
	private final IssueAssigneeRepository issueAssigneeRepository;
	@Autowired
	private final MemberRepository memberRepository;
	@Autowired
	private final WorkspaceRepository workspaceRepository;
	@Autowired
	private final WorkspaceMemberRepository workspaceMemberRepository;
	@Autowired
	private final InvitationRepository invitationRepository;
	@Autowired
	private final PasswordEncoder passwordEncoder;

	/**
	 * Creates a workspace with a specified number of members
	 * The specified number does not include the OWNER
	 *
	 * @param numberOfMembers The number of members to be added to the workspace
	 * @param workspacePassword The password for the workspace
	 * @param issueKeyPrefix The prefix for generating issue keys
	 * @return The created Workspace
	 */
	public Workspace createWorkspaceWithMembers(
		int numberOfMembers,
		String workspacePassword,
		String issueKeyPrefix
	) {
		Member owner = createMember("owner");

		Workspace workspace = createWorkspace("test workspace", workspacePassword, issueKeyPrefix);

		createWorkspaceMember(owner, workspace, WorkspaceRole.OWNER);

		for (int i = 0; i < numberOfMembers; i++) {
			Member member = createMember("member" + i);
			createWorkspaceMember(member, workspace, WorkspaceRole.MEMBER);
		}

		return workspace;
	}

	public Member createMember(String loginId) {
		return memberRepository.save(
			Member.builder()
				.loginId(loginId)
				.email(loginId + "@test.com")
				.username(loginId + "Username")
				.password(passwordEncoder.encode("test1234!"))
				.name("Gildong Hong")
				.build()
		);
	}

	public Member createTestMember(Long id, String loginId) {
		return memberRepository.save(new MemberBuilderForTest()
			.id(id)
			.loginId(loginId)
			.email(loginId + "@test.com")
			.username(loginId)
			.password(passwordEncoder.encode("test1234!"))
			.build());
	}

	/**
	 * Creates and saves a workspace
	 *
	 * @param name The name of the workspace
	 * @param password The password of the workspace (null is allowed, pass null if not exist, encrypt if exist)
	 * @param issueKeyPrefix The prefix for generating issue keys
	 * @return The created Workspace
	 */
	public Workspace createWorkspace(
		String name,
		String password,
		String issueKeyPrefix
	) {
		// TODO: change after improving Workspace password logic
		String encodedPassword = password != null ? passwordEncoder.encode(password) : null;

		return workspaceRepository.save(
			Workspace.builder()
				.name(name)
				.description("description")
				.password(encodedPassword)
				.code(RandomStringUtils.randomAlphanumeric(8)) // 워크스페이스의 8자리 코드 (Base62, 중복 비허용)
				.issueKeyPrefix(issueKeyPrefix)
				.build()
		);
	}

	/**
	 * Creates and saves a WorkspaceMember
	 *
	 * @param member Member
	 * @param workspace Workspace
	 * @param role The WorkspaceRole of the member
	 * @return The created WorkspaceMember
	 */
	public WorkspaceMember createWorkspaceMember(
		Member member,
		Workspace workspace,
		WorkspaceRole role
	) {
		WorkspaceMember workspaceMember = WorkspaceMember.createWorkspaceMember(
			member,
			workspace,
			role
		);

		return workspaceMemberRepository.save(workspaceMember);
	}

	/**
	 * Creates a new issue of the specified type and assigns members to it
	 *
	 * @param type The type of the issue (e.g., EPIC, STORY, BUG, TASK, SUB_TASK)
	 * @param workspace The workspace in which the issue will be created
	 * @param title The title of the issue
	 * @param priority The priority level of the issue
	 * @param dueAt The due date and time for the issue
	 * @param workspaceMembers A list of workspace members to be assigned to the issue
	 * @return The created issue
	 */
	public Issue createIssueWithAssignees(
		IssueType type,
		Workspace workspace,
		String title,
		IssuePriority priority,
		LocalDateTime dueAt,
		List<WorkspaceMember> workspaceMembers
	) {
		Issue issue = switch (type) {
			case EPIC -> createEpic(workspace, title, priority, dueAt);
			case STORY -> createStory(workspace, title, priority, dueAt);
			case BUG -> createBug(workspace, title, priority, dueAt);
			case TASK -> createTask(workspace, title, priority, dueAt);
			case SUB_TASK -> createSubTask(workspace, title, priority, dueAt);
		};

		addIssueAssignees(issue, workspaceMembers);

		return issueRepository.save(issue);
	}

	public Epic createEpic(
		Workspace workspace,
		String title,
		IssuePriority priority,
		LocalDateTime dueAt
	) {
		Epic epic = Epic.builder()
			.workspace(workspace)
			.title(title)
			.content("epic content")
			.priority(priority)
			.dueAt(dueAt)
			.businessGoal("business goal")
			.build();

		return issueRepository.save(epic);
	}

	public Story createStory(
		Workspace workspace,
		String title,
		IssuePriority priority,
		LocalDateTime dueAt
	) {
		Story story = Story.builder()
			.workspace(workspace)
			.title(title)
			.content("story content")
			.priority(priority)
			.dueAt(dueAt)
			.userStory("user story")
			.acceptanceCriteria("acceptance criteria")
			.build();

		return issueRepository.save(story);
	}

	public Bug createBug(
		Workspace workspace,
		String title,
		IssuePriority priority,
		LocalDateTime dueAt
	) {
		Bug bug = Bug.builder()
			.workspace(workspace)
			.title(title)
			.content("bug content")
			.priority(priority)
			.dueAt(dueAt)
			.reproducingSteps("bug reproduce steps")
			.severity(BugSeverity.MAJOR)
			.build();

		return issueRepository.save(bug);
	}

	public Task createTask(
		Workspace workspace,
		String title,
		IssuePriority priority,
		LocalDateTime dueAt
	) {
		Task task = Task.builder()
			.workspace(workspace)
			.title(title)
			.content("task content")
			.priority(priority)
			.dueAt(dueAt)
			.build();

		return issueRepository.save(task);
	}

	public SubTask createSubTask(
		Workspace workspace,
		String title,
		IssuePriority priority,
		LocalDateTime dueAt
	) {
		SubTask subTask = SubTask.builder()
			.workspace(workspace)
			.title(title)
			.content("sub task content")
			.priority(priority)
			.dueAt(dueAt)
			.build();

		return issueRepository.save(subTask);
	}

	public List<IssueAssignee> addIssueAssignees(
		Issue issue,
		List<WorkspaceMember> workspaceMembers
	) {
		List<IssueAssignee> issueAssignees = workspaceMembers.stream()
			.map(workspaceMember -> new IssueAssignee(issue, workspaceMember))
			.toList();

		issueAssigneeRepository.saveAll(issueAssignees);

		return issueAssignees;
	}

	public IssueAssignee addIssueAssignee(
		Issue issue,
		WorkspaceMember workspaceMember
	) {
		IssueAssignee assignee = issue.addAssignee(workspaceMember);
		return issueAssigneeRepository.save(assignee);
	}

	public List<IssueReviewer> addIssueReviewers(
		Issue issue,
		List<WorkspaceMember> workspaceMembers
	) {
		List<IssueReviewer> issueReviewers = workspaceMembers.stream()
			.map(workspaceMember -> new IssueReviewer(workspaceMember, issue))
			.toList();

		issueReviewerRepository.saveAll(issueReviewers);

		return issueReviewers;
	}

	public IssueReviewer addIssueReviewer(
		Issue issue,
		WorkspaceMember workspaceMember
	) {
		IssueReviewer reviewer = issue.addReviewer(workspaceMember);
		return issueReviewerRepository.save(reviewer);
	}

	public Review createReview(
		IssueReviewer issueReviewer,
		String title,
		ReviewStatus status
	) {
		return reviewRepository.save(Review.builder()
			.issueReviewer(issueReviewer)
			.title(title)
			.content("review content")
			.status(status)
			.build());
	}

	public IssueComment createIssueComment(
		Issue issue,
		String content,
		WorkspaceMember author,
		Comment parentComment
	) {
		return commentRepository.save(IssueComment.builder()
			.issue(issue)
			.content(content)
			.author(author)
			.parentComment(parentComment)
			.build());
	}

	public ReviewComment createReviewComment(
		Review review,
		String content,
		WorkspaceMember author,
		Comment parentComment
	) {
		return commentRepository.save(ReviewComment.builder()
			.review(review)
			.content(content)
			.author(author)
			.parentComment(parentComment)
			.build());
	}

	public Invitation createInvitation(
		Workspace workspace,
		Member member,
		InvitationStatus status
	) {
		Invitation invitation = Invitation.builder()
			.workspace(workspace)
			.member(member)
			.status(status)
			.build();

		return invitationRepository.save(invitation);
	}
}
