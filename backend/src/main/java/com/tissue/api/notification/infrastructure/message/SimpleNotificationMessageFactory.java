package com.tissue.api.notification.infrastructure.message;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.notification.domain.model.vo.NotificationMessage;
import com.tissue.api.notification.domain.service.message.NotificationContentArgumentsFormatter;
import com.tissue.api.notification.domain.service.message.NotificationMessageFactory;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberFinder;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SimpleNotificationMessageFactory implements NotificationMessageFactory {

	private final MessageSource messageSource;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final NotificationContentArgumentsFormatter argumentFormatter;

	@Override
	public <T extends DomainEvent> NotificationMessage createMessage(T event) {
		// 현재 로케일 결정
		Locale locale = LocaleContextHolder.getLocale();

		// 메시지 키 생성
		String titleKey = "notification." + event.getNotificationType().name() + ".title";
		String contentKey = "notification." + event.getNotificationType().name() + ".content";

		// 기본 파라미터 준비
		WorkspaceMember actor = workspaceMemberFinder.findWorkspaceMember(
			event.getActorMemberId(),
			event.getWorkspaceCode()
		);

		String actorNickname = actor.getDisplayName();

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
	 * NotificationContentArgumentFormatter를 활용하여 이벤트 타입에 맞는 인자 배열 생성
	 */
	private <T extends DomainEvent> Object[] createContentArguments(T event, String actorNickname) {
		return switch (event.getNotificationType()) {
			case ISSUE_CREATED, ISSUE_UPDATED, ISSUE_COMMENT_ADDED,
				ISSUE_ASSIGNED, ISSUE_UNASSIGNED, ISSUE_REVIEW_REQUESTED ->
				argumentFormatter.createStandardArgs(actorNickname, event.getEntityKey());

			case ISSUE_STATUS_CHANGED -> argumentFormatter.createIssueStatusChangeArgs(event, actorNickname);

			case ISSUE_PARENT_ASSIGNED -> argumentFormatter.createIssueParentAssignedArgs(event, actorNickname);

			case ISSUE_PARENT_REMOVED -> argumentFormatter.createIssueParentRemovedArgs(event, actorNickname);

			case ISSUE_REVIEWER_ADDED -> argumentFormatter.createReviewerAddedArgs(event, actorNickname);

			// case ISSUE_REVIEW_SUBMITTED -> argumentFormatter.createReviewSubmittedArgs(event, actorNickname);
			//
			// case REVIEW_COMMENT_ADDED -> argumentFormatter.createReviewCommentAddedArgs(event, actorNickname);

			case SPRINT_STARTED -> argumentFormatter.createSprintStartedArgs(event.getEntityKey());

			case SPRINT_COMPLETED -> argumentFormatter.createSprintCompletedArgs(event);

			case MEMBER_JOINED_WORKSPACE -> argumentFormatter.createMemberJoinedWorkspaceArgs(event);

			case WORKSPACE_MEMBER_ROLE_CHANGED ->
				argumentFormatter.createWorkspaceRoleChangedArgs(event, actorNickname);

			default -> argumentFormatter.createStandardArgs(actorNickname, event.getEntityKey());
		};
	}
}
