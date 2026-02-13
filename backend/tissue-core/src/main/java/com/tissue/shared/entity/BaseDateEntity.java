package com.tissue.shared.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseDateEntity {

    // TODO: id 필드도 그냥 여기서 관리하는걸 고려

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant lastModifiedAt;

    public abstract Long getId();

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
        BaseDateEntity that = (BaseDateEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return effectiveClass(this).hashCode();
    }
}
