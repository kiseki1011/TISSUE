package com.tissue.shared.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class SoftDeleteEntity extends BaseDateEntity {

    @CreatedBy
    @Column(updatable = false)
    private Long createdBy;

    @LastModifiedBy
    private Long lastModifiedBy;

    @Column(nullable = false)
    private boolean archived = false;

    @Column(nullable = false)
    private boolean softDeleted = false;

    @Nullable
    private Instant archivedAt;

    @Nullable
    private Instant softDeletedAt;

    /**
     * Marks this entity as soft-deleted.
     *
     * <p>This operation internally calls {@link #archive()} to enforce a read-only state,
     * ensuring that deleted resources cannot be further modified.
     */
    public void softDelete() {
        if (!softDeleted) {
            this.softDeleted = true;
            this.softDeletedAt = Instant.now();
            archive();
        }
    }

    public void archive() {
        if (!archived) {
            this.archived = true;
            this.archivedAt = Instant.now();
        }
    }

    public void restoreSoftDeleted() {
        if (!softDeleted) {
            return;
        }
        this.softDeleted = false;
        this.softDeletedAt = null;
        restoreArchived();
    }

    public void restoreArchived() {
        if (!archived) {
            return;
        }
        this.archived = false;
        this.archivedAt = null;
    }
}
