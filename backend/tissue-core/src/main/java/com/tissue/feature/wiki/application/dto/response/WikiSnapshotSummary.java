package com.tissue.feature.wiki.application.dto.response;

import com.tissue.feature.wiki.domain.WikiDocumentSnapshot;
import com.tissue.feature.wiki.domain.enums.SemanticUpdateType;
import java.time.Instant;
import lombok.Builder;

@Builder
public record WikiSnapshotSummary(
        Long id,
        String snapshotVersion,
        SemanticUpdateType updateType,
        String editReason,
        String snapshotTitle,
        Long createdBy,
        Instant createdAt) {

    public static WikiSnapshotSummary from(WikiDocumentSnapshot snapshot) {
        return WikiSnapshotSummary.builder()
                .id(snapshot.getId())
                .snapshotVersion(snapshot.getSnapshotVersion().toString())
                .updateType(snapshot.getUpdateType())
                .editReason(snapshot.getEditReason())
                .snapshotTitle(snapshot.getSnapshotTitle())
                .createdBy(snapshot.getCreatedBy())
                .createdAt(snapshot.getCreatedAt())
                .build();
    }
}
