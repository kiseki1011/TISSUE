package com.tissue.issue.application.port.in;

import com.tissue.issue.application.dto.request.AddIssueRelationCommand;
import com.tissue.issue.application.dto.request.RemoveIssueRelationCommand;

public interface IssueRelationUseCase {

    // TODO: 관계를 형성하는 타켓 이슈가 다른 프로젝트에 존재하는 경우 권한을 어떻게 처리할까?
    void add(AddIssueRelationCommand cmd);

    void remove(RemoveIssueRelationCommand cmd);
}
