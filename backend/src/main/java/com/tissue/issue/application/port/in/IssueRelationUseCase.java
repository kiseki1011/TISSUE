package com.tissue.issue.application.port.in;

import com.tissue.issue.application.dto.request.AddIssueRelationCommand;
import com.tissue.issue.application.dto.request.RemoveIssueRelationCommand;

public interface IssueRelationUseCase {

    // TODO: if the target issue for the relation is in different project, how should i handle permissions?
    //  option 1: just allow it
    //  option 2: must be at least a ProjectRole.VIEWER(or MEMBER) for the other project
    void add(AddIssueRelationCommand cmd);

    void remove(RemoveIssueRelationCommand cmd);
}
