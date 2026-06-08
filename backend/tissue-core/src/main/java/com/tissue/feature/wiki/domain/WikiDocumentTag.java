package com.tissue.feature.wiki.domain;

import com.tissue.shared.entity.HardDeleteEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Getter
@Table(
        name = "wiki_document_tag",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_wiki_document_tag",
                    columnNames = {"wiki_document_id", "wiki_tag_id"})
        },
        indexes = {
            @Index(name = "idx_wiki_document_tag_document_id", columnList = "wiki_document_id"),
            @Index(name = "idx_wiki_document_tag_tag_id", columnList = "wiki_tag_id")
        })
public class WikiDocumentTag extends HardDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wiki_document_id", nullable = false)
    private WikiDocument document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wiki_tag_id", nullable = false)
    private WikiTag tag;

    @SuppressWarnings("NullAway.Init")
    protected WikiDocumentTag() {}

    public WikiDocumentTag(WikiDocument document, WikiTag tag) {
        this.document = document;
        this.tag = tag;
    }
}
