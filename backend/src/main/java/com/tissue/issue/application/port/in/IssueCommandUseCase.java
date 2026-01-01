package com.tissue.issue.application.port.in;

import com.tissue.issue.application.dto.request.AssignParentCommand;
import com.tissue.issue.application.dto.request.CreateIssueCommand;
import com.tissue.issue.application.dto.request.DeleteIssueCommand;
import com.tissue.issue.application.dto.request.RemoveParentCommand;
import com.tissue.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.issue.application.dto.request.UpdateCustomFieldsCommand;
import com.tissue.issue.application.dto.request.UpdateStoryPointCommand;
import com.tissue.issue.application.dto.response.IssueCreateResponse;

public interface IssueCommandUseCase {

    IssueCreateResponse create(CreateIssueCommand cmd);

    void updateCommonFields(UpdateCommonFieldsCommand cmd);

    void updateCustomFields(UpdateCustomFieldsCommand cmd);

    void updateStoryPoint(UpdateStoryPointCommand cmd);

    void assignParent(AssignParentCommand cmd);

    void removeParent(RemoveParentCommand cmd);

    void softDelete(DeleteIssueCommand cmd);

    // TODO: restore()
    // TODO: approve()
    //   - reject(), requestChange()도 추가해야 하나?
    // TODO: batchChangeParent()
    // TODO: batchSoftDelete()
    // TODO: cloneIssue()
    //  - 특정 이슈 내용 복사해서 새로 생성? 필요한지는 모르겠네...
    //  - 필요해도 아마 다른 프로젝트로 특정 이슈를 복사하는 것 정도?
}
