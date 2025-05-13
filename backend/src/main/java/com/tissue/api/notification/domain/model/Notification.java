package com.tissue.api.notification.domain.model;

import java.util.UUID;

import com.tissue.api.common.entity.BaseDateEntity;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.model.vo.EntityReference;
import com.tissue.api.notification.domain.model.vo.NotificationMessage;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
			columnNames = {"eventId", "receiverMemberId"})
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
	private Long receiverMemberId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NotificationType type;

	@Embedded
	private EntityReference entityReference;

	@Embedded
	private NotificationMessage message;

	@Column(nullable = false)
	private Long actorMemberId;

	private String actorNickname;

	@Column(nullable = false)
	private boolean isRead;

	@Builder
	public Notification(
		UUID eventId,
		NotificationType notificationType,
		EntityReference entityReference,
		Long actorMemberId,
		String actorNickname,
		Long receiverMemberId,
		NotificationMessage message
	) {
		this.eventId = eventId;
		this.type = notificationType;
		this.entityReference = entityReference;
		this.actorMemberId = actorMemberId;
		this.actorNickname = actorNickname;
		this.receiverMemberId = receiverMemberId;
		this.message = message;
		this.isRead = false;
	}

	public void markAsRead() {
		this.isRead = true;
	}

	public String getTitle() {
		return message.title();
	}

	public String getContent() {
		return message.content();
	}
}
