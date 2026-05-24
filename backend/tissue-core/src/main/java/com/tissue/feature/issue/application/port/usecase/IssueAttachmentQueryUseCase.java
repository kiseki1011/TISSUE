package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.application.dto.response.FileDownloadResult;
import com.tissue.feature.issue.application.dto.response.IssueAttachmentDetailResponse;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.List;

public interface IssueAttachmentQueryUseCase {

    List<IssueAttachmentDetailResponse> getIssueAttachments(IssueIdentifier iid, Long actorMemberId);

    FileDownloadResult download(IssueIdentifier iid, Long attachmentId, Long actorMemberId);
}
