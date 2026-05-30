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
    public WikiDocumentDetail getDocumentDetail(Long wikiId, Long actorMemberId) {
        WikiDocument document = wikiDocumentQueryRepository
                .findWithParentById(wikiId)
                .orElseThrow(() -> new WikiDocumentNotFoundException(wikiId));

        List<WikiLink> links = wikiLinkRepository.findBySourceDocumentId(wikiId);

        return WikiDocumentDetail.from(document, links);
    }

    @Override
    public List<WikiDocumentSummary> getRootDocuments(Long actorMemberId) {
        List<WikiDocument> roots = wikiDocumentQueryRepository.findRootDocuments();
        Set<Long> idsWithChildren = wikiDocumentQueryRepository.findDocumentIdsWithChildren();

        return roots.stream()
                .map(doc -> WikiDocumentSummary.from(doc, idsWithChildren.contains(doc.getId())))
                .toList();
    }

    @Override
    public List<WikiDocumentSummary> getChildrenDocuments(Long parentWikiId, Long actorMemberId) {
        wikiDocumentFinder.getById(parentWikiId);

        List<WikiDocument> children = wikiDocumentQueryRepository.findChildrenByParentId(parentWikiId);
        Set<Long> idsWithChildren = wikiDocumentQueryRepository.findDocumentIdsWithChildren();

        return children.stream()
                .map(doc -> WikiDocumentSummary.from(doc, idsWithChildren.contains(doc.getId())))
                .toList();
    }

    @Override
    public List<WikiDocumentTreeNode> getDocumentTree(Long actorMemberId) {
        List<WikiDocument> allDocuments = wikiDocumentQueryRepository.findAllWithParent();

        return allDocuments.stream().map(WikiDocumentTreeNode::from).toList();
    }

    @Override
    public List<WikiSnapshotSummary> getVersionHistory(Long wikiId, Long actorMemberId) {
        wikiDocumentFinder.getById(wikiId);

        List<WikiDocumentSnapshot> snapshots = wikiSnapshotRepository.findByDocumentIdOrderByVersionDesc(wikiId);

        return snapshots.stream().map(WikiSnapshotSummary::from).toList();
    }

    @Override
    public WikiSnapshotDetail getVersionSnapshotDetail(Long wikiId, Long snapshotId, Long actorMemberId) {
        wikiDocumentFinder.getById(wikiId);

        WikiDocumentSnapshot snapshot = wikiSnapshotRepository
                .findById(snapshotId)
                .orElseThrow(() -> new WikiSnapshotNotFoundException(wikiId, snapshotId));

        return WikiSnapshotDetail.from(snapshot);
    }

    @Override
    public KeysetPageResponse<WikiDocumentSearchResult> searchDocuments(
            String keyword,
            Long actorMemberId,
            @Nullable Instant keysetModifiedAt,
            @Nullable Long keysetDocumentId,
            int limit) {
        List<WikiDocument> documents =
                wikiSearchRepository.searchByKeyword(keyword, keysetModifiedAt, keysetDocumentId, limit);

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
