package com.tissue.feature.workspace.application.port.usecase;

import com.tissue.feature.workspace.application.dto.response.query.InvitationDetail;
import java.util.List;

public interface InvitationUseCase {

    void accept(Long memberId, Long invitationId);

    void reject(Long memberId, Long invitationId);

    List<InvitationDetail> getMyInvitations(Long memberId);
}
