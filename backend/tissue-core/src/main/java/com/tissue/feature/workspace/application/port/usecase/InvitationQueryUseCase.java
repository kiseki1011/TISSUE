package com.tissue.feature.workspace.application.port.usecase;

import com.tissue.feature.workspace.application.dto.response.query.InvitationDetail;
import java.util.List;

public interface InvitationQueryUseCase {

    List<InvitationDetail> getMyInvitations(Long memberId);
}
