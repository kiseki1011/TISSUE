package com.tissue.api.notification.domain;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.issue.domain.event.IssueParentAssignedEvent;
import com.tissue.api.issue.domain.event.IssueStatusChangedEvent;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
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
		WorkspaceMember actor = workspaceMemberReader.findWorkspaceMember(event.getTriggeredByWorkspaceMemberId());
		String actorNickname = actor.getNickname();

		// 파라미터 배열 생성
		Object[] titleArgs = createTitleArguments(event, actorNickname);
		Object[] contentArgs = createContentArguments(event, actorNickname);

		// 메시지 가져오기
		String title = messageSource.getMessage(titleKey, titleArgs, titleKey, locale);
		String content = messageSource.getMessage(contentKey, contentArgs, contentKey, locale);

		return new NotificationMessage(title, content);
	}

	/**
	 * 알림 제목용 파라미터 배열 생성
	 */
	private <T extends DomainEvent> Object[] createTitleArguments(T event, String actorNickname) {
		// 대부분의 알림 제목에는 워크스페이스 코드와 엔티티 키만 필요
		return new Object[] {event.getWorkspaceCode(), event.getEntityKey()};
	}

	/**
	 * 알림 내용용 파라미터 배열 생성
	 */
	private <T extends DomainEvent> Object[] createContentArguments(T event, String actorNickname) {
		return switch (event.getNotificationType()) {
			case ISSUE_CREATED -> new Object[] {actorNickname, event.getEntityKey()};
			case ISSUE_UPDATED -> new Object[] {actorNickname, event.getEntityKey()};
			case ISSUE_STATUS_CHANGED -> {
				if (event instanceof IssueStatusChangedEvent issueStatusChangedEvent) {
					yield new Object[] {
						actorNickname,
						event.getEntityKey(),
						issueStatusChangedEvent.getOldStatus().toString(),
						issueStatusChangedEvent.getNewStatus().toString()
					};
				}
				yield new Object[] {actorNickname, event.getEntityKey()};
			}
			case ISSUE_PARENT_ASSIGNED -> {
				if (event instanceof IssueParentAssignedEvent issueParentAssignedEvent) {
					yield new Object[] {
						actorNickname,
						event.getEntityKey(),
						issueParentAssignedEvent.getParentIssueKey()
					};
				}
				yield new Object[] {actorNickname, event.getEntityKey()};
			}
			case ISSUE_PARENT_REMOVED -> new Object[] {actorNickname, event.getEntityKey()};
			case ISSUE_COMMENT_ADDED -> new Object[] {actorNickname, event.getEntityKey()};
			case ISSUE_ASSIGNED -> new Object[] {actorNickname, event.getEntityKey()};
			// case ISSUE_REVIEWER_ADDED -> {
			// 	if (event instanceof IssueReviewerAddedEvent issueReviewerAddedEvent) {
			// 		yield new Object[] {
			// 			actorNickname,
			// 			event.getEntityKey(),
			// 			issueReviewerAddedEvent.getTargetNickname()
			// 		};
			// 	}
			// 	yield new Object[] {actorNickname, event.getEntityKey()};
			// }
			case REVIEW_REQUESTED -> new Object[] {actorNickname, event.getEntityKey()};
			// case REVIEW_SUBMITTED -> {
			// 	if (event instanceof ReviewSubmittedEvent reviewSubmittedEvent) {
			// 		yield new Object[] {
			// 			actorNickname,
			// 			event.getEntityKey(),
			// 			reviewSubmittedEvent.getReviewStatus()
			// 		};
			// 	}
			// 	yield new Object[] {actorNickname, event.getEntityKey()};
			// }
			// case REVIEW_COMMENT_ADDED -> {
			// 	if (event instanceof ReviewCommentAddedEvent reviewCommentAddedEvent) {
			// 		yield new Object[] {
			// 			actorNickname,
			// 			event.getEntityKey(),
			// 			reviewCommentAddedEvent.getReviewKey()
			// 		};
			// 	}
			// 	yield new Object[] {actorNickname, event.getEntityKey()};
			// }
			// case MEMBER_JOINED_WORKSPACE -> {
			// 	if (event instanceof MemberJoinedWorkspaceEvent memberJoinedWorkspaceEvent) {
			// 		yield new Object[] {
			// 			actorNickname,
			// 			event.getWorkspaceCode(),
			// 			event.getActorRole()
			// 		};
			// 	}
			// 	yield new Object[] {actorNickname, event.getEntityKey()};
			// }

			// case WORKSPACE_MEMBER_ROLE_CHANGED -> {
			// 	if (event instanceof WorkspaceMemberRoleChangedEvent roleEvent) {
			// 		yield new Object[] {
			// 			actorNickname,
			// 			roleEvent.getWorkspaceMemberNickname(),
			// 			roleEvent.getOldRole().toString(),
			// 			roleEvent.getNewRole().toString()
			// 		};
			// 	}
			// 	yield new Object[] {actorNickname, event.getEntityKey()};
			// }
			case SPRINT_STARTED -> new Object[] {event.getEntityKey()};
			// case SPRINT_ENDED -> {
			// 	if (event instanceof SprintEndedEvent sprintEndedEvent) {
			// 		yield new Object[] {
			// 			actorNickname,
			// 			event.getEntityKey(),
			// 			event.getPeriodString()
			// 		};
			// 	}
			// 	yield new Object[] {actorNickname, event.getEntityKey()};
			// }
			default -> new Object[] {actorNickname, event.getEntityKey()};
		};

	}
}
