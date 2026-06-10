package com.tissue.feature.wiki.domain;

import com.tissue.feature.wiki.domain.enums.WikiLinkTargetType;
import com.tissue.feature.wiki.domain.exception.WikiErrorCode;
import com.tissue.shared.entity.HardDeleteEntity;
import com.tissue.shared.exception.base.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.Getter;

@Entity
@Getter
@Table(
        name = "wiki_link",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_wiki_link_source_target",
                    columnNames = {"source_document_id", "target_type", "target_id"})
        })
public class WikiLink extends HardDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_document_id", nullable = false)
    private WikiDocument sourceDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private WikiLinkTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @SuppressWarnings("NullAway.Init")
    protected WikiLink() {}

    public static WikiLink create(WikiDocument document, WikiLinkTargetType targetType, Long targetId) {
        ensureNotSelfReference(document, targetType, targetId);

        WikiLink link = new WikiLink();
        link.sourceDocument = document;
        link.targetType = targetType;
        link.targetId = targetId;

        return link;
    }

    private static void ensureNotSelfReference(WikiDocument document, WikiLinkTargetType targetType, Long targetId) {
        if (targetType == WikiLinkTargetType.WIKI_DOC && Objects.equals(document.getId(), targetId)) {
            throw new BadRequestException(WikiErrorCode.LINK_SELF_REFERENCE);
        }
    }
}
