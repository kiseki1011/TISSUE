package com.tissue.api.notification.domain;

import java.util.UUID;

import com.tissue.api.common.entity.BaseDateEntity;
import com.tissue.api.notification.domain.enums.NotificationEntityType;
import com.tissue.api.notification.domain.enums.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
	uniqueConstraints = {
		@UniqueConstraint(
			name = "UK_EVENT_RECEIVER",
			columnNames = {"eventId", "receiverWorkspaceMemberId"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseDateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event_id", nullable = false)
	private UUID eventId;

	@Column(nullable = false)
	private Long receiverWorkspaceMemberId;

	@Column(nullable = false)
	private String workspaceCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NotificationType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NotificationEntityType entityType;

	// Todo
	//  - 처음 표현 계층에서 받은 식별자(예를 들어서 workspaceCode + issueKey)들을 통해서 처음 조회 후,
	//  - 이후 부터는 id 조회하도록 리팩토링
	//  - EntityId라는 VO를 만들어서 안에 workspaceCode, xxxKey 등의 정보를 담는 방식 고려
	@Column(nullable = false)
	private Long entityId;

	@Column(nullable = false)
	private String title;

	@Column(length = 1000)
	private String message;

	@Column(nullable = false)
	private Long actorWorkspaceMemberId;

	private String actorWorkspaceMemberNickname;

	@Column(nullable = false)
	private boolean isRead;

	@Builder
	public Notification(
		UUID eventId,
		String workspaceCode,
		NotificationType type,
		NotificationEntityType entityType,
		Long entityId,
		Long actorWorkspaceMemberId,
		String actorWorkspaceMemberNickname,
		Long receiverWorkspaceMemberId,
		String title,
		String message
	) {
		this.eventId = eventId;
		this.workspaceCode = workspaceCode;
		this.type = type;
		this.entityType = entityType;
		this.entityId = entityId;
		this.actorWorkspaceMemberId = actorWorkspaceMemberId;
		this.actorWorkspaceMemberNickname = actorWorkspaceMemberNickname;
		this.receiverWorkspaceMemberId = receiverWorkspaceMemberId;
		this.title = title;
		this.message = message;
		this.isRead = false;
	}

	public void markAsRead() {
		this.isRead = true;
	}
}
