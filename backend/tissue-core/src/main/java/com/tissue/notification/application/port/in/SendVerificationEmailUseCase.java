package com.tissue.notification.application.port.in;

import com.tissue.member.domain.event.VerificationEmailRequestedEvent;

public interface SendVerificationEmailUseCase {
    void sendVerificationEmail(VerificationEmailRequestedEvent event);
}
