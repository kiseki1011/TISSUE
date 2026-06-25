package com.tissue.feature.wiki.domain;

import com.tissue.feature.wiki.domain.enums.SemanticUpdateType;
import com.tissue.feature.wiki.domain.exception.WikiErrorCode;
import com.tissue.feature.wiki.domain.policy.WikiTagConstraintPolicy;
import com.tissue.feature.wiki.domain.vo.SnapshotVersion;
import com.tissue.shared.entity.SoftDeleteEntity;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

@LLMGenerated(
    llmInvolvement = LLMInvolvement.ASSISTED,
    model = "claude-opus-4-8",
    evaluation = Evaluation.ACCEPTABLE,
    evaluationReason = "(Only the searchVector part was assisted with AI) Manually tested, Tested with "
        + "human written integration tests. But still needs inspection. Also a load test for performance "
        + "check will be good to do.",
    reviewedBy = "kiseki1011"
)
@Entity
@Getter
@SQLRestriction("soft_deleted = false")
public class WikiDocument extends SoftDeleteEntity {

    @Version
    private Long version;

    @Embedded
    private SnapshotVersion currentSnapshotVersion;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Generated tsvector column produced from title + content, owned by PostgreSQL
     * (see {@code tissue-bootstrap/src/main/resources/db/fts.sql} for the DDL). The mapping exists
     * only so Specifications can reference it via {@code root.get("searchVector")} inside
     * {@code fts_match()} / {@code fts_rank()} calls; the value is never read from Java.
     * byte[] (VARBINARY) avoids Hibernate's tsvector -> String conversion failure.
     */
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "search_vector", insertable = false, updatable = false, columnDefinition = "tsvector")
    private byte[] searchVector;

    @Column(name = "locked", nullable = false)
    private boolean locked;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_document_id")
    private WikiDocument parentDocument;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WikiDocumentTag> tags = new HashSet<>();

    @SuppressWarnings("NullAway.Init")
    protected WikiDocument() {}

    public static WikiDocument create(String title, String content, @Nullable WikiDocument parentDocument) {
        WikiDocument wikiDocument = new WikiDocument();
        wikiDocument.currentSnapshotVersion = SnapshotVersion.initial();
        wikiDocument.locked = false;
        wikiDocument.title = title;
        wikiDocument.content = content;
        wikiDocument.setParent(parentDocument);

        return wikiDocument;
    }

    public void updateTitle(String title) {
        ensureEditable();
        this.title = title;
    }

    public void updateContent(String content, SemanticUpdateType versionUpdateType) {
        ensureEditable();
        this.content = content;
        this.currentSnapshotVersion = currentSnapshotVersion.bumpVersion(versionUpdateType);
    }

    public void setParent(@Nullable WikiDocument parentDocument) {
        this.parentDocument = parentDocument;
    }

    public void addTag(WikiTag tag) {
        ensureEditable();
        boolean alreadyTagged =
                tags.stream().anyMatch(documentTag -> documentTag.getTag().equals(tag));
        if (alreadyTagged) {
            return;
        }
        if (tags.size() >= WikiTagConstraintPolicy.MAX_TAGS_PER_DOCUMENT) {
            throw new ResourceConflictException(WikiErrorCode.DOCUMENT_TAG_LIMIT_EXCEEDED);
        }
        tags.add(new WikiDocumentTag(this, tag));
    }

    public void removeTag(WikiTag tag) {
        ensureEditable();
        tags.removeIf(documentTag -> documentTag.getTag().equals(tag));
    }

    public void lock() {
        this.locked = true;
    }

    public void unLock() {
        this.locked = false;
    }

    private void ensureEditable() {
        if (this.locked) {
            throw new BadRequestException(WikiErrorCode.DOCUMENT_LOCKED);
        }
    }
}
