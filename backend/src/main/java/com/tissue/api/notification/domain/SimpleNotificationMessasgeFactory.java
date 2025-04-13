package com.tissue.api.notification.domain;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import com.tissue.api.comment.domain.event.ReviewCommentAddedEvent;
import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.issue.domain.event.IssueParentAssignedEvent;
import com.tissue.api.issue.domain.event.IssueParentRemovedEvent;
import com.tissue.api.issue.domain.event.IssueStatusChangedEvent;
import com.tissue.api.review.domain.event.ReviewSubmittedEvent;
import com.tissue.api.review.domain.event.ReviewerAddedEvent;
import com.tissue.api.sprint.domain.event.SprintCompletedEvent;
import com.tissue.api.workspace.domain.event.MemberJoinedWorkspaceEvent;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.event.WorkspaceMemberRoleChangedEvent;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SimpleNotificationMessasgeFactory implements NotificationMessageFactory {

	private final MessageSource messageSource;
	private final WorkspaceMemberReader workspaceMemberReader;

	@Override
	public <T extends DomainEvent> NotificationMessage createMessage(T event) {
		// 현재 로케일 결정
		Locale locale = LocaleContextHolder.getLocale();

		// 메시지 키 생성
		String titleKey = "notification." + event.getNotificationType().name() + ".title";
		String contentKey = "notification." + event.getNotificationType().name() + ".content";

		// 기본 파라미터 준비
		// TODO: 굳이 레포지토리로 조회해서 actor를 준비해놔야할까?
		//  -> 그냥 event에서 꺼내서 사용해도 되지 않을까?
		WorkspaceMember actor = workspaceMemberReader.findWorkspaceMember(event.getTriggeredByWorkspaceMemberId());
		String actorNickname = actor.getNickname();

		// 파라미터 배열 생성
		Object[] titleArgs = createTitleArguments(event);
		Object[] contentArgs = createContentArguments(event, actorNickname);

		// 메시지 가져오기
		String title = messageSource.getMessage(titleKey, titleArgs, titleKey, locale);
		String content = messageSource.getMessage(contentKey, contentArgs, contentKey, locale);

		return new NotificationMessage(title, content);
	}

	/**
	 * 알림 제목용 파라미터 배열 생성
	 */
	private <T extends DomainEvent> Object[] createTitleArguments(T event) {
		return new Object[] {event.getWorkspaceCode(), event.getEntityKey()};
	}

	/**
	 * 알림 내용용 파라미터 배열 생성
	 */
	private <T extends DomainEvent> Object[] createContentArguments(T event, String actorNickname) {
		return switch (event.getNotificationType()) {
			case ISSUE_CREATED,
				ISSUE_UPDATED,
				ISSUE_COMMENT_ADDED,
				ISSUE_ASSIGNED,
				ISSUE_UNASSIGNED,
				ISSUE_REVIEW_REQUESTED -> new Object[] {actorNickname, event.getEntityKey()};

			case ISSUE_STATUS_CHANGED -> {
				IssueStatusChangedEvent statusChangedEvent = (IssueStatusChangedEvent)event;
				yield new Object[] {
					actorNickname,
					event.getEntityKey(),
					statusChangedEvent.getOldStatus().toString(),
					statusChangedEvent.getNewStatus().toString()
				};
			}

			case ISSUE_PARENT_ASSIGNED -> {
				IssueParentAssignedEvent parentAssignedEvent = (IssueParentAssignedEvent)event;
				yield new Object[] {
					actorNickname,
					event.getEntityKey(),
					parentAssignedEvent.getParentIssueKey()
				};
			}

			case ISSUE_PARENT_REMOVED -> {
				IssueParentRemovedEvent parentRemovedEvent = (IssueParentRemovedEvent)event;
				yield new Object[] {
					actorNickname,
					event.getEntityKey(),
					parentRemovedEvent.getRemovedParentIssueKey()
				};
			}

			case ISSUE_REVIEWER_ADDED -> {
				ReviewerAddedEvent reviewerAddedEvent = (ReviewerAddedEvent)event;
				yield new Object[] {
					actorNickname,
					event.getEntityKey(),
					reviewerAddedEvent.getReviewerNickname()
				};
			}

			case ISSUE_REVIEW_SUBMITTED -> {
				ReviewSubmittedEvent reviewSubmittedEvent = (ReviewSubmittedEvent)event;
				yield new Object[] {
					actorNickname,
					event.getEntityKey(),
					reviewSubmittedEvent.getReviewStatus()
				};
			}

			// TODO: 리뷰 id 대신 제목을 파라미터로 넘기는게 좋지 않을까?(10자 자르기)
			case REVIEW_COMMENT_ADDED -> {
				ReviewCommentAddedEvent commentEvent = (ReviewCommentAddedEvent)event;
				yield new Object[] {
					actorNickname,
					event.getEntityKey(),
					commentEvent.getReviewId().toString()
				};
			}

			case SPRINT_STARTED -> new Object[] {event.getEntityKey()};

			case SPRINT_COMPLETED -> {
				SprintCompletedEvent sprintCompletedEvent = (SprintCompletedEvent)event;
				yield new Object[] {
					event.getEntityKey(),
					sprintCompletedEvent.getSprintStartedAt().toString(),
					sprintCompletedEvent.getSprintCompletedAt().toString()
				};
			}

			case MEMBER_JOINED_WORKSPACE -> {
				MemberJoinedWorkspaceEvent joinedWorkspaceEvent = (MemberJoinedWorkspaceEvent)event;
				yield new Object[] {
					joinedWorkspaceEvent.getNickname(),
					joinedWorkspaceEvent.getWorkspaceRole().toString()
				};
			}

			case WORKSPACE_MEMBER_ROLE_CHANGED -> {
				WorkspaceMemberRoleChangedEvent roleChangedEvent = (WorkspaceMemberRoleChangedEvent)event;
				yield new Object[] {
					actorNickname,
					roleChangedEvent.getTargetNickname(),
					roleChangedEvent.getOldRole().toString(),
					roleChangedEvent.getNewRole().toString()
				};
			}
			default -> new Object[] {actorNickname, event.getEntityKey()};
		};
	}
}
