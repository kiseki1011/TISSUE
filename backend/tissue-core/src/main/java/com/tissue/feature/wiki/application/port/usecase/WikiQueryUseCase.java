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

    WikiDocumentDetail getDocumentDetail(String workspaceKey, Long wikiId, Long actorMemberId);

    List<WikiDocumentSummary> getRootDocuments(String workspaceKey, Long actorMemberId);

    List<WikiDocumentSummary> getChildrenDocuments(String workspaceKey, Long parentWikiId, Long actorMemberId);

    List<WikiDocumentTreeNode> getDocumentTree(String workspaceKey, Long actorMemberId);

    List<WikiSnapshotSummary> getVersionHistory(String workspaceKey, Long wikiId, Long actorMemberId);

    WikiSnapshotDetail getVersionSnapshotDetail(String workspaceKey, Long wikiId, Long snapshotId, Long actorMemberId);

    KeysetPageResponse<WikiDocumentSearchResult> searchDocuments(
            String workspaceKey,
            String keyword,
            Long actorMemberId,
            @Nullable Instant keysetModifiedAt,
            @Nullable Long keysetDocumentId,
            int limit);
}
