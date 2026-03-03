package com.tissue.feature.member.domain;

import com.tissue.shared.entity.BaseDateEntity;
import com.tissue.shared.enums.SupportedLanguage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.Getter;

@Entity
@Getter
@Table(
        name = "member",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_member_email", columnNames = "email"),
            @UniqueConstraint(name = "uk_member_username", columnNames = "username")
        })
public class Member extends BaseDateEntity {

    @Column(name = "email", nullable = false)
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

    @SuppressWarnings("NullAway.Init")
    protected Member() {}

    public static Member create(String email, String username, String name) {
        Member member = new Member();
        member.email = Objects.requireNonNullElse(email, "");
        member.username = Objects.requireNonNullElse(username, "");
        member.name = Objects.requireNonNullElse(name, "");
        member.status = MemberStatus.ACTIVE;
        member.role = SystemRole.USER;
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
    }
}
