package com.tissue.shared.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant lastModifiedAt;

    /**
     * Provides a hibernate-safe implementation of {@code equals(o)} and {@code hashCode()}.
     * Compares entities by ID and supports Hibernate lazy-loaded proxies.
     *
     * <p>Implemented based on the link below.
     *
     * @param o the reference object with which to compare.
     * @return {@code true} if this object is the same as param o; {@code false} otherwise.
     *
     * @see <a href="https://jpa-buddy.com/blog/hopefully-the-final-article-about-equals-and-hashcode-for-jpa-entities-with-db-generated-ids/">
     *     https://jpa-buddy.com/blog/hopefully-the-final-article-about-equals-and-hashcode-for-jpa-entities-with-db-generated-ids/</a>
     */
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
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public final int hashCode() {
        return effectiveClass(this).hashCode();
    }

    /**
     * Resolves the underlying class if the object is a Hibernate proxy.
     */
    private static Class<?> effectiveClass(Object o) {
        if (o instanceof HibernateProxy p) {
            return p.getHibernateLazyInitializer().getPersistentClass();
        }
        return o.getClass();
    }
}
