package com.tissue.feature.wiki.application.port.usecase;

import com.tissue.feature.wiki.application.dto.response.WikiDocumentDetail;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentSearchResult;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentSummary;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentTreeNode;
import com.tissue.feature.wiki.application.dto.response.WikiSnapshotDetail;
import com.tissue.feature.wiki.application.dto.response.WikiSnapshotSummary;
import com.tissue.shared.dto.KeysetPageResponse;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface WikiQueryUseCase {

    WikiDocumentDetail getDocumentDetail(Long wikiId, Long actorMemberId);

    List<WikiDocumentSummary> getRootDocuments(Long actorMemberId);

    List<WikiDocumentSummary> getChildrenDocuments(Long parentWikiId, Long actorMemberId);

    List<WikiDocumentTreeNode> getDocumentTree(Long actorMemberId);

    List<WikiSnapshotSummary> getVersionHistory(Long wikiId, Long actorMemberId);

    WikiSnapshotDetail getVersionSnapshotDetail(Long wikiId, Long snapshotId, Long actorMemberId);

    KeysetPageResponse<WikiDocumentSearchResult> searchDocuments(
            String keyword,
            Long actorMemberId,
            @Nullable Instant keysetModifiedAt,
            @Nullable Long keysetDocumentId,
            int limit);
}
