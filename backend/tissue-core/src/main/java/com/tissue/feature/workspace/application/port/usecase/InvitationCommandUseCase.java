package com.tissue.feature.workspace.application.port.usecase;

public interface InvitationCommandUseCase {

    void accept(Long memberId, Long invitationId);

    void reject(Long memberId, Long invitationId);
}
