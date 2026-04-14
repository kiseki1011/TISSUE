package com.tissue.feature.wiki.application.dto.response;

import com.tissue.feature.wiki.domain.WikiDocumentSnapshot;
import com.tissue.feature.wiki.domain.enums.SemanticUpdateType;
import java.time.Instant;
import lombok.Builder;

@Builder
public record WikiSnapshotDetail(
        Long id,
        Long documentId,
        String snapshotVersion,
        SemanticUpdateType updateType,
        String editReason,
        String snapshotTitle,
        String snapshotContent,
        Long createdBy,
        Instant createdAt) {

    public static WikiSnapshotDetail from(WikiDocumentSnapshot snapshot) {
        return WikiSnapshotDetail.builder()
                .id(snapshot.getId())
                .documentId(snapshot.getDocument().getId())
                .snapshotVersion(snapshot.getSnapshotVersion().toString())
                .updateType(snapshot.getUpdateType())
                .editReason(snapshot.getEditReason())
                .snapshotTitle(snapshot.getSnapshotTitle())
                .snapshotContent(snapshot.getSnapshotContent())
                .createdBy(snapshot.getCreatedBy())
                .createdAt(snapshot.getCreatedAt())
                .build();
    }
}
