package com.tissue.feature.member.domain;

import com.tissue.shared.entity.BaseDateEntity;
import com.tissue.shared.enums.SupportedLanguage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(
        name = "member",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_member_email", columnNames = "email"),
            @UniqueConstraint(name = "uk_member_username", columnNames = "username")
        })
public class Member extends BaseDateEntity {

    @Nullable
    @Column(name = "email")
    private String email;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    private SupportedLanguage language = SupportedLanguage.EN;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_status", nullable = false)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = false)
    private SystemRole role;

    @Nullable
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @SuppressWarnings("NullAway.Init")
    protected Member() {}

    public static Member create(String email, String username, String name) {
        return buildWithEmail(email, username, name, SystemRole.USER);
    }

    public static Member createAsAdmin(String email, String username, String name) {
        return buildWithEmail(email, username, name, SystemRole.ADMIN);
    }

    public static Member createWithoutEmail(String username, String name) {
        return buildWithoutEmail(username, name, SystemRole.USER);
    }

    public static Member createAsAdminWithoutEmail(String username, String name) {
        return buildWithoutEmail(username, name, SystemRole.ADMIN);
    }

    private static Member buildWithEmail(String email, String username, String name, SystemRole role) {
        Member member = new Member();
        member.email = Objects.requireNonNullElse(email, "");
        member.username = Objects.requireNonNullElse(username, "");
        member.name = Objects.requireNonNullElse(name, "");
        member.status = MemberStatus.ACTIVE;
        member.role = role;
        return member;
    }

    private static Member buildWithoutEmail(String username, String name, SystemRole role) {
        Member member = new Member();
        member.email = null;
        member.username = Objects.requireNonNullElse(username, "");
        member.name = Objects.requireNonNullElse(name, "");
        member.status = MemberStatus.ACTIVE;
        member.role = role;
        return member;
    }

    public void updateEmail(String email) {
        this.email = Objects.requireNonNullElse(email, "");
    }

    public void updateUsername(String username) {
        this.username = Objects.requireNonNullElse(username, "");
    }

    public void updateName(String name) {
        this.name = Objects.requireNonNullElse(name, "");
    }

    public void updateLanguage(SupportedLanguage language) {
        this.language = language;
    }

    public void activate() {
        this.status = MemberStatus.ACTIVE;
    }

    public void withdraw() {
        this.status = MemberStatus.DELETED;
        this.deletedAt = Instant.now();
    }

    /**
     * Strips PII from this member and transitions to {@link MemberStatus#PURGED}.
     * The row is kept so that {@code WorkspaceMember} still has a stable FK target.
     */
    public void anonymize() {
        this.email = null;
        this.username = "deleted_" + getId();
        this.name = "Deleted User";
        this.status = MemberStatus.PURGED;
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return status == MemberStatus.DELETED;
    }

    public boolean isPurged() {
        return status == MemberStatus.PURGED;
    }
}
