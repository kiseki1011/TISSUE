package com.tissue.api.notification.domain.model;

import com.tissue.api.notification.domain.enums.NotificationChannel;
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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	uniqueConstraints = {
		@UniqueConstraint(
			name = "UK_NOTIFICATION_PREF",
			columnNames = {"receiverMemberId", "workspaceKey", "type", "channel"})
	}
)
public class NotificationPreference {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long receiverMemberId;

	@Column(nullable = false)
	private String workspaceCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NotificationType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NotificationChannel channel;

	@Column(nullable = false)
	private boolean enabled = true;

	@Builder
	public NotificationPreference(
		Long receiverMemberId,
		String workspaceCode,
		NotificationType type,
		NotificationChannel channel,
		boolean enabled
	) {
		this.receiverMemberId = receiverMemberId;
		this.workspaceCode = workspaceCode;
		this.type = type;
		this.channel = channel;
		this.enabled = enabled;
	}

	public void updateEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
