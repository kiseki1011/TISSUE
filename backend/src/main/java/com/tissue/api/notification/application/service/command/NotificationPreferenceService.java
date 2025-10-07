package com.tissue.api.notification.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.notification.domain.enums.NotificationChannel;
import com.tissue.api.notification.domain.model.NotificationPreference;
import com.tissue.api.notification.infrastructure.repository.NotificationPreferenceRepository;
import com.tissue.api.notification.presentation.dto.request.UpdateNotificationPreferenceRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

	private final NotificationPreferenceRepository repository;

	// TODO: 현재는 EMAIL로 채널이 하드코딩. 추후 채널이 늘어나면 request에서 꺼내서 사용하기.
	@Transactional
	public void updatePreference(
		String workspaceKey,
		Long memberId,
		UpdateNotificationPreferenceRequest request
	) {
		NotificationPreference pref = repository.findByReceiver(
			memberId,
			workspaceKey,
			request.type(),
			NotificationChannel.EMAIL
		).orElse(NotificationPreference.builder()
			.receiverMemberId(memberId)
			.workspaceKey(workspaceKey)
			.type(request.type())
			.channel(NotificationChannel.EMAIL)
			.build()
		);

		pref.updateEnabled(request.enabled());

		repository.save(pref);
	}
}
