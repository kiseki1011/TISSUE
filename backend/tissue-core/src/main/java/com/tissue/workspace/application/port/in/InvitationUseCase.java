package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.response.query.InvitationDetail;
import java.util.List;

public interface InvitationUseCase {

    void accept(Long memberId, Long invitationId);

    void reject(Long memberId, Long invitationId);

    List<InvitationDetail> getMyInvitations(Long memberId);
}
