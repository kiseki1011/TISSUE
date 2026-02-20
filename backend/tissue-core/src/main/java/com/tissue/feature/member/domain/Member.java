package com.tissue.feature.member.domain;

import com.tissue.feature.workspace.domain.Invitation;
import com.tissue.shared.entity.BaseDateEntity;
import com.tissue.shared.enums.SupportedLanguage;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

@Entity
@Getter
public class Member extends BaseDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", unique = true, nullable = false)
    private String email = "";

    @Column(name = "username", unique = true, nullable = false)
    private String username = "";

    @Column(name = "name", nullable = false)
    private String name = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    private SupportedLanguage language = SupportedLanguage.EN;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_status", nullable = false)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = false)
    private SystemRole role;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Invitation> invitations = new ArrayList<>();

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

    // TODO: active -> activate
    public void active() {
        this.status = MemberStatus.ACTIVE;
    }

    public void withdraw() {
        this.status = MemberStatus.DELETED;
    }

    // TODO: add lock?
}
