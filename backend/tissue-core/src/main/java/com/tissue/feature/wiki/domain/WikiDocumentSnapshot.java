package com.tissue.feature.wiki.domain;

import com.tissue.feature.wiki.domain.enums.SemanticUpdateType;
import com.tissue.feature.wiki.domain.vo.SnapshotVersion;
import com.tissue.shared.entity.HardDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(
        name = "wiki_document_snapshot",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"wiki_document_id", "snapshot_version"})})
public class WikiDocumentSnapshot extends HardDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wiki_document_id", nullable = false)
    private WikiDocument document;

    @Embedded
    private SnapshotVersion snapshotVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "semantic_update_type", nullable = false)
    private SemanticUpdateType updateType;

    @Column(name = "edit_reason", nullable = false)
    private String editReason;

    @Column(name = "workspace_key", nullable = false, updatable = false)
    private String workspaceKey;

    @Column(name = "title", nullable = false, updatable = false)
    private String snapshotTitle;

    @Lob
    @Column(name = "content", nullable = false, updatable = false)
    private String snapshotContent;

    @SuppressWarnings("NullAway.Init")
    protected WikiDocumentSnapshot() {}

    public static WikiDocumentSnapshot create(
            WikiDocument document, SemanticUpdateType updateType, @Nullable String editReason) {
        WikiDocumentSnapshot snapshot = new WikiDocumentSnapshot();
        snapshot.snapshotVersion = document.getCurrentSnapshotVersion();
        snapshot.updateType = updateType;
        snapshot.editReason = Objects.requireNonNullElse(editReason, "");
        snapshot.workspaceKey = document.getWorkspaceKey();
        snapshot.snapshotTitle = document.getTitle();
        snapshot.snapshotContent = document.getContent();

        return snapshot;
    }
}
