package com.tissue.feature.wiki.application.port.usecase;

import com.tissue.feature.wiki.application.dto.response.FileDownloadResult;
import com.tissue.feature.wiki.application.dto.response.WikiAttachmentDetailResponse;
import com.tissue.feature.wiki.application.dto.response.WikiAttachmentUploadResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface WikiAttachmentUseCase {

    WikiAttachmentUploadResponse uploadFile(Long wikiId, MultipartFile file, Long actorMemberId);

    void deleteAttachment(Long wikiId, Long attachmentId, Long actorMemberId);

    List<WikiAttachmentDetailResponse> getWikiAttachments(Long wikiId, Long actorMemberId);

    FileDownloadResult download(Long wikiId, Long attachmentId, Long actorMemberId);
}
