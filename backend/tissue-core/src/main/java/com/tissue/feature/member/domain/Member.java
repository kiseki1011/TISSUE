package com.tissue.feature.member.domain;

import com.tissue.feature.agent.domain.AgentType;
import com.tissue.feature.agent.model.domain.AiModel;
import com.tissue.feature.organization.position.domain.Position;
import com.tissue.feature.organization.team.domain.Team;
import com.tissue.shared.entity.BaseDateEntity;
import com.tissue.shared.enums.SupportedLanguage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
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

    @Version
    private Long version;

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

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_type", nullable = false)
    private MemberType memberType = MemberType.HUMAN;

    // TODO: Separate to AgentProfile VO
    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Member owner;

    @Nullable
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type")
    private AgentType agentType;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private AiModel model;

    @Nullable
    @Column(name = "description")
    private String description;

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

    public static Member createAsSuperAdmin(String email, String username, String name) {
        return buildWithEmail(email, username, name, SystemRole.SUPER_ADMIN);
    }

    public static Member createAsSuperAdminWithoutEmail(String username, String name) {
        return buildWithoutEmail(username, name, SystemRole.SUPER_ADMIN);
    }

    public static Member createAgent(
            Member owner,
            String username,
            String name,
            @Nullable AgentType agentType,
            @Nullable AiModel model,
            @Nullable String description) {
        Member member = new Member();
        member.email = null;
        member.username = Objects.requireNonNull(username);
        member.name = Objects.requireNonNull(name);
        member.status = MemberStatus.ACTIVE;
        member.role = SystemRole.USER;
        member.memberType = MemberType.AGENT;
        member.owner = Objects.requireNonNull(owner, "An agent must have an owner");
        member.agentType = Objects.requireNonNullElse(agentType, AgentType.GENERAL);
        member.model = model;
        member.description = description;
        return member;
    }

    private static Member buildWithEmail(String email, String username, String name, SystemRole role) {
        Member member = new Member();
        member.email = Objects.requireNonNull(email);
        member.username = Objects.requireNonNull(username);
        member.name = Objects.requireNonNull(name);
        member.status = MemberStatus.ACTIVE;
        member.role = role;
        return member;
    }

    /**
     * Only for logic when {@code tissue.security.email-required} is false.
     *
     * <p>Check the {@code .env} or {@code application-*.yml}.
     */
    private static Member buildWithoutEmail(String username, String name, SystemRole role) {
        Member member = new Member();
        member.email = null;
        member.username = Objects.requireNonNull(username);
        member.name = Objects.requireNonNull(name);
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

    public void restore() {
        if (status != MemberStatus.DELETED) {
            throw new IllegalStateException(
                    "Cannot restore a member that is not DELETED (current status: " + status + ")");
        }
        this.status = MemberStatus.ACTIVE;
        this.deletedAt = null;
    }

    public void lock() {
        this.status = MemberStatus.LOCKED;
    }

    public void unlock() {
        this.status = MemberStatus.ACTIVE;
    }

    /**
     * Strips PII from this member and transitions to {@link MemberStatus#PURGED}.
     *
     * <p>The row is kept so that {@code ProjectMember} / {@code Issue} / {@code Comment} FKs still have
     * a stable target.
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

    public boolean isLocked() {
        return status == MemberStatus.LOCKED;
    }

    public boolean isPurged() {
        return status == MemberStatus.PURGED;
    }

    public boolean isSuperAdmin() {
        return role == SystemRole.SUPER_ADMIN;
    }

    public boolean isAgent() {
        return memberType == MemberType.AGENT;
    }

    public boolean isHuman() {
        return memberType == MemberType.HUMAN;
    }

    public boolean hasAtLeast(SystemRole required) {
        return role.isEqualOrHigherThan(required);
    }

    public void changeRole(SystemRole newRole) {
        this.role = Objects.requireNonNull(newRole);
    }

    public void assignPosition(@Nullable Position position) {
        this.position = position;
    }

    public void assignTeam(@Nullable Team team) {
        this.team = team;
    }

    public void assignModel(@Nullable AiModel model) {
        this.model = model;
    }

    public void updateAgentType(@Nullable AgentType agentType) {
        this.agentType = Objects.requireNonNullElse(agentType, AgentType.GENERAL);
    }

    public void updateDescription(@Nullable String description) {
        this.description = description;
    }
}
