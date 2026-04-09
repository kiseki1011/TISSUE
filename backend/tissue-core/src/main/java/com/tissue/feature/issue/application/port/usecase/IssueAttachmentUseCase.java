package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.application.dto.response.FileDownloadResult;
import com.tissue.feature.issue.application.dto.response.IssueAttachmentDetailResponse;
import com.tissue.feature.issue.application.dto.response.IssueAttachmentUploadResponse;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface IssueAttachmentUseCase {

    IssueAttachmentUploadResponse upload(IssueIdentifier iid, MultipartFile file, Long memberId);

    void delete(IssueIdentifier iid, Long attachmentId, Long memberId);

    List<IssueAttachmentDetailResponse> getIssueAttachments(IssueIdentifier iid, Long memberId);

    FileDownloadResult download(IssueIdentifier iid, Long attachmentId, Long memberId);
}
