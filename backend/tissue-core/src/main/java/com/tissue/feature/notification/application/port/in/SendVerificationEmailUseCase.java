package com.tissue.feature.notification.application.port.in;

import com.tissue.feature.member.domain.event.VerificationEmailRequestedEvent;

public interface SendVerificationEmailUseCase {
    void sendVerificationEmail(VerificationEmailRequestedEvent event);
}
