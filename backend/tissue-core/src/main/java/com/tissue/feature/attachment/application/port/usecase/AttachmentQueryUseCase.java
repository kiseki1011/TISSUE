package com.tissue.feature.attachment.application.port.usecase;

import com.tissue.feature.attachment.application.dto.response.AttachmentDetailResponse;
import com.tissue.feature.attachment.application.dto.response.FileDownloadResult;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.List;

public interface AttachmentQueryUseCase {

    List<AttachmentDetailResponse> getIssueAttachments(IssueIdentifier iid, Long memberId);

    FileDownloadResult download(IssueIdentifier iid, Long attachmentId, Long memberId);
}
