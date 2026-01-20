package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.in.JoinViaLinkCommand;
import com.tissue.workspace.application.dto.out.command.WorkspaceMemberResponse;

public interface WorkspaceLinkJoinUseCase {

    WorkspaceMemberResponse joinViaLink(JoinViaLinkCommand cmd);
}
