package com.tissue.feature.wiki.domain;

import com.tissue.shared.entity.BaseDateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Getter
@Table(
        name = "wiki_bookmark",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"member_id", "wiki_document_id"})})
public class WikiBookmark extends BaseDateEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wiki_document_id", nullable = false)
    private WikiDocument document;

    @Column(name = "workspace_key", nullable = false)
    private String workspaceKey;

    @SuppressWarnings("NullAway.Init")
    protected WikiBookmark() {}

    public static WikiBookmark create(Long memberId, WikiDocument document) {
        WikiBookmark bookmark = new WikiBookmark();
        bookmark.memberId = memberId;
        bookmark.document = document;
        bookmark.workspaceKey = document.getWorkspaceKey();
        return bookmark;
    }
}
