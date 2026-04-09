package com.tissue.feature.wiki.domain;

import com.tissue.feature.wiki.domain.exception.WikiErrorCode;
import com.tissue.shared.entity.HardDeleteEntity;
import com.tissue.shared.exception.base.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import lombok.Getter;

@Entity
@Getter
public class WikiAttachment extends HardDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wiki_document_id", nullable = false)
    private WikiDocument document;

    @Column(name = "workspace_key", nullable = false, updatable = false)
    private String workspaceKey;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String storedFilename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private String storedPath;

    @SuppressWarnings("NullAway.Init")
    protected WikiAttachment() {}

    public static WikiAttachment create(
            WikiDocument document,
            String originalFilename,
            String storedFilename,
            String contentType,
            long fileSize,
            String storedPath) {
        WikiAttachment attachment = new WikiAttachment();
        attachment.document = document;
        attachment.ensureEditable();
        attachment.workspaceKey = document.getWorkspaceKey();
        attachment.originalFilename = originalFilename;
        attachment.storedFilename = storedFilename;
        attachment.contentType = contentType;
        attachment.fileSize = fileSize;
        attachment.storedPath = storedPath;

        return attachment;
    }

    public boolean isUploader(Long memberId) {
        return Objects.equals(getCreatedBy(), memberId);
    }

    private void ensureEditable() {
        if (document.isLocked()) {
            throw new BadRequestException(WikiErrorCode.DOCUMENT_LOCKED);
        }
    }
}
