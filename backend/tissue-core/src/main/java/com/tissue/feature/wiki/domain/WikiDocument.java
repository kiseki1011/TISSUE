package com.tissue.feature.wiki.domain;

import com.tissue.feature.wiki.domain.enums.SemanticUpdateType;
import com.tissue.feature.wiki.domain.exception.WikiErrorCode;
import com.tissue.feature.wiki.domain.vo.SnapshotVersion;
import com.tissue.shared.entity.SoftDeleteEntity;
import com.tissue.shared.exception.base.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

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

    @Column(name = "locked", nullable = false)
    private boolean locked;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_document_id")
    private WikiDocument parentDocument;

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
