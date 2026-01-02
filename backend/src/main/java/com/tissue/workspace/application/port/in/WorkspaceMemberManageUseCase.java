package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.in.ManagePositionCommand;
import com.tissue.workspace.application.dto.in.ManageTeamCommand;
import com.tissue.workspace.application.dto.in.UpdateDisplayNameCommand;
import com.tissue.workspace.application.dto.in.UpdateRoleCommand;

public interface WorkspaceMemberManageUseCase {

    void updateDisplayName(UpdateDisplayNameCommand cmd);

    void updateRole(UpdateRoleCommand cmd);

    void addPosition(ManagePositionCommand cmd);

    void removePosition(ManagePositionCommand cmd);

    void addTeam(ManageTeamCommand cmd);

    void removeTeam(ManageTeamCommand cmd);
}
