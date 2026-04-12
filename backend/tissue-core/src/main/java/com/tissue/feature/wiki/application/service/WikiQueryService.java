package com.tissue.feature.wiki.application.service;

import com.tissue.feature.wiki.application.dto.response.WikiDocumentDetail;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentSearchResult;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentSummary;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentTreeNode;
import com.tissue.feature.wiki.application.dto.response.WikiSnapshotDetail;
import com.tissue.feature.wiki.application.dto.response.WikiSnapshotSummary;
import com.tissue.feature.wiki.application.port.repository.WikiDocumentQueryRepository;
import com.tissue.feature.wiki.application.port.repository.WikiLinkRepository;
import com.tissue.feature.wiki.application.port.repository.WikiSearchRepository;
import com.tissue.feature.wiki.application.port.repository.WikiSnapshotRepository;
import com.tissue.feature.wiki.application.port.usecase.WikiQueryUseCase;
import com.tissue.feature.wiki.application.service.finder.WikiDocumentFinder;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.wiki.domain.WikiDocumentSnapshot;
import com.tissue.feature.wiki.domain.WikiLink;
import com.tissue.feature.wiki.domain.exception.WikiDocumentNotFoundException;
import com.tissue.feature.wiki.domain.exception.WikiSnapshotNotFoundException;
import com.tissue.shared.dto.KeysetPageResponse;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WikiQueryService implements WikiQueryUseCase {

    private final WikiDocumentQueryRepository wikiDocumentQueryRepository;
    private final WikiSnapshotRepository wikiSnapshotRepository;
    private final WikiLinkRepository wikiLinkRepository;
    private final WikiSearchRepository wikiSearchRepository;
    private final WikiDocumentFinder wikiDocumentFinder;

    @Override
    public WikiDocumentDetail getDocumentDetail(String workspaceKey, Long wikiId, Long actorMemberId) {
        WikiDocument document = wikiDocumentQueryRepository
                .findWithParentByWorkspaceKeyAndId(workspaceKey, wikiId)
                .orElseThrow(() -> new WikiDocumentNotFoundException(workspaceKey, wikiId));

        List<WikiLink> links = wikiLinkRepository.findBySourceDocumentIdAndWorkspaceKey(wikiId, workspaceKey);

        return WikiDocumentDetail.from(document, links);
    }

    @Override
    public List<WikiDocumentSummary> getRootDocuments(String workspaceKey, Long actorMemberId) {
        List<WikiDocument> roots = wikiDocumentQueryRepository.findRootDocuments(workspaceKey);
        Set<Long> idsWithChildren = wikiDocumentQueryRepository.findDocumentIdsWithChildren(workspaceKey);

        return roots.stream()
                .map(doc -> WikiDocumentSummary.from(doc, idsWithChildren.contains(doc.getId())))
                .toList();
    }

    @Override
    public List<WikiDocumentSummary> getChildrenDocuments(String workspaceKey, Long parentWikiId, Long actorMemberId) {
        wikiDocumentFinder.getBy(workspaceKey, parentWikiId);

        List<WikiDocument> children = wikiDocumentQueryRepository.findChildrenByParentId(workspaceKey, parentWikiId);
        Set<Long> idsWithChildren = wikiDocumentQueryRepository.findDocumentIdsWithChildren(workspaceKey);

        return children.stream()
                .map(doc -> WikiDocumentSummary.from(doc, idsWithChildren.contains(doc.getId())))
                .toList();
    }

    @Override
    public List<WikiDocumentTreeNode> getDocumentTree(String workspaceKey, Long actorMemberId) {
        List<WikiDocument> allDocuments = wikiDocumentQueryRepository.findAllWithParentByWorkspaceKey(workspaceKey);

        return allDocuments.stream().map(WikiDocumentTreeNode::from).toList();
    }

    @Override
    public List<WikiSnapshotSummary> getVersionHistory(String workspaceKey, Long wikiId, Long actorMemberId) {
        wikiDocumentFinder.getBy(workspaceKey, wikiId);

        List<WikiDocumentSnapshot> snapshots =
                wikiSnapshotRepository.findByDocumentIdOrderByVersionDesc(wikiId, workspaceKey);

        return snapshots.stream().map(WikiSnapshotSummary::from).toList();
    }

    @Override
    public WikiSnapshotDetail getVersionSnapshotDetail(
            String workspaceKey, Long wikiId, Long snapshotId, Long actorMemberId) {
        wikiDocumentFinder.getBy(workspaceKey, wikiId);

        WikiDocumentSnapshot snapshot = wikiSnapshotRepository
                .findByIdAndWorkspaceKey(snapshotId, workspaceKey)
                .orElseThrow(() -> new WikiSnapshotNotFoundException(wikiId, snapshotId));

        return WikiSnapshotDetail.from(snapshot);
    }

    @Override
    public KeysetPageResponse<WikiDocumentSearchResult> searchDocuments(
            String workspaceKey,
            String keyword,
            Long actorMemberId,
            @Nullable Instant keysetModifiedAt,
            @Nullable Long keysetDocumentId,
            int limit) {
        List<WikiDocument> documents =
                wikiSearchRepository.searchByKeyword(workspaceKey, keyword, keysetModifiedAt, keysetDocumentId, limit);

        List<WikiDocumentSearchResult> content = documents.stream()
                .map(doc -> WikiDocumentSearchResult.from(doc, keyword))
                .toList();

        Long nextKeysetId = null;
        Instant nextKeysetModifiedAt = null;
        if (!content.isEmpty()) {
            WikiDocumentSearchResult last = content.getLast();
            nextKeysetId = last.id();
            nextKeysetModifiedAt = last.lastModifiedAt();
        }

        return KeysetPageResponse.of(content, nextKeysetId, nextKeysetModifiedAt);
    }
}
