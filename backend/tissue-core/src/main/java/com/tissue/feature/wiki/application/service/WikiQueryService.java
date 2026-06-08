package com.tissue.feature.wiki.application.service;

import com.tissue.feature.wiki.application.dto.WikiSearchCursor;
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
import com.tissue.shared.dto.Cursor;
import com.tissue.shared.dto.CursorPage;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
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

    @LLMGenerated(
            llmInvolvement = LLMInvolvement.ASSISTED,
            model = "claude-opus-4-8",
            evaluation = Evaluation.NOT_REVIEWED,
            evaluationReason = "Passes test, but code not reviewed.",
            reviewedBy = "kiseki1011")
    @Override
    public CursorPage<WikiDocumentSearchResult> searchDocuments(
            @Nullable String keyword,
            @Nullable Set<Long> tagIds,
            Long actorMemberId,
            @Nullable String cursor,
            int limit) {
        WikiSearchCursor decoded = Cursor.decode(cursor, WikiSearchCursor.class);
        Instant keysetModifiedAt = (decoded != null) ? Instant.parse(decoded.lastModifiedAt()) : null;
        Long keysetId = (decoded != null) ? decoded.id() : null;

        List<WikiDocument> rows = wikiSearchRepository.search(keyword, tagIds, keysetModifiedAt, keysetId, limit + 1);

        boolean hasNext = rows.size() > limit;
        List<WikiDocument> pageRows = hasNext ? rows.subList(0, limit) : rows;

        List<WikiDocumentSearchResult> content = pageRows.stream()
                .map(doc -> WikiDocumentSearchResult.from(doc, keyword))
                .toList();

        String nextCursor = null;
        if (hasNext) {
            WikiDocumentSearchResult last = content.getLast();
            nextCursor =
                    Cursor.encode(new WikiSearchCursor(last.lastModifiedAt().toString(), last.id()));
        }

        return CursorPage.of(content, nextCursor);
    }
}
