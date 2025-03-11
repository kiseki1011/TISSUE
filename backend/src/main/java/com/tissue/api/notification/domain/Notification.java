package com.tissue.api.notification.domain;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseDateEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

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

	// EntityReference 라는 복합 식별자 클래스를 만들까? -> entityId, entityKey 모두 기록
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
		Long receiverWorkspaceMemberId,
		String workspaceCode,
		NotificationType type,
		NotificationEntityType entityType,
		Long entityId,
		String title,
		String message,
		Long actorWorkspaceMemberId,
		String actorWorkspaceMemberNickname
	) {
		this.receiverWorkspaceMemberId = receiverWorkspaceMemberId;
		this.workspaceCode = workspaceCode;
		this.type = type;
		this.entityType = entityType;
		this.entityId = entityId;
		this.title = title;
		this.message = message;
		this.actorWorkspaceMemberId = actorWorkspaceMemberId;
		this.actorWorkspaceMemberNickname = actorWorkspaceMemberNickname;
		this.isRead = false;
	}

	public void markAsRead() {
		this.isRead = true;
	}
}
