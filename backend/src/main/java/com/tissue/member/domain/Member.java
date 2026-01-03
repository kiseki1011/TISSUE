package com.tissue.member.domain;

import com.tissue.common.entity.BaseDateEntity;
import com.tissue.security.authorization.SystemRole;
import com.tissue.workspace.domain.Invitation;
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
import lombok.Getter;

@Entity
@Getter
public class Member extends BaseDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = false)
    private SystemRole role;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Invitation> invitations = new ArrayList<>();

    @SuppressWarnings("NullAway.Init")
    protected Member() {}

    /**
     * 회원을 생성. (비밀번호는 AuthIdentity에서 별도로 관리)
     */
    public static Member create(String email, String username, String name) {
        Member member = new Member();
        member.email = email;
        member.username = username;
        member.name = name;
        member.status = MemberStatus.ACTIVE;
        member.role = SystemRole.USER;

        return member;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void updateUsername(String username) {
        this.username = username;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void active() {
        this.status = MemberStatus.ACTIVE;
    }

    public void withdraw() {
        this.status = MemberStatus.DELETED;
    }
}
