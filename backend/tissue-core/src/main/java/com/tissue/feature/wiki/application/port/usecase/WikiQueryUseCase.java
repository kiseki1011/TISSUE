package com.tissue.feature.wiki.application.port.usecase;

import com.tissue.feature.wiki.application.dto.response.WikiDocumentDetail;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentSearchResult;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentSummary;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentTreeNode;
import com.tissue.feature.wiki.application.dto.response.WikiSnapshotDetail;
import com.tissue.feature.wiki.application.dto.response.WikiSnapshotSummary;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;

public interface WikiQueryUseCase {

    WikiDocumentDetail getDocumentDetail(Long wikiId, Long actorMemberId);

    List<WikiDocumentSummary> getRootDocuments(Long actorMemberId);

    List<WikiDocumentSummary> getChildrenDocuments(Long parentWikiId, Long actorMemberId);

    List<WikiDocumentTreeNode> getDocumentTree(Long actorMemberId);

    List<WikiSnapshotSummary> getVersionHistory(Long wikiId, Long actorMemberId);

    WikiSnapshotDetail getVersionSnapshotDetail(Long wikiId, Long snapshotId, Long actorMemberId);

    Page<WikiDocumentSearchResult> searchDocuments(
            @Nullable String keyword, @Nullable Set<Long> tagIds, Long actorMemberId, int page, int size);
}
