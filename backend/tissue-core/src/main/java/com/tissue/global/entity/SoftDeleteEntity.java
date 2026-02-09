package com.tissue.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.Objects;
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

    public abstract Long getId();

    // TODO: add javadoc that explains why archive() is called within
    //  - archive means that modification is prohibited and the resource is read-only
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

    // TODO: add javadoc that explains this is a hibernate-safe implementation of equals and hashCode
    private static Class<?> effectiveClass(Object o) {
        if (o instanceof org.hibernate.proxy.HibernateProxy p) {
            return p.getHibernateLazyInitializer().getPersistentClass();
        }
        return o.getClass();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (effectiveClass(this) != effectiveClass(o)) {
            return false;
        }
        SoftDeleteEntity that = (SoftDeleteEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return effectiveClass(this).hashCode();
    }
}
