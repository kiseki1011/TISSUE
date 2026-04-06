package com.tissue.feature.workspace.domain;

import static com.tissue.feature.workspace.domain.enums.WorkspaceRole.ADMIN;
import static com.tissue.feature.workspace.domain.policy.WorkspaceConstraintPolicy.KEY_MAX_LENGTH;
import static com.tissue.feature.workspace.domain.policy.WorkspaceConstraintPolicy.KEY_MIN_LENGTH;
import static com.tissue.feature.workspace.domain.policy.WorkspaceConstraintPolicy.KEY_REGEX;

import com.tissue.feature.workspace.domain.exception.WorkspaceArchivedException;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.shared.entity.SoftDeleteEntity;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ForbiddenException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(
        name = "workspace",
        uniqueConstraints = {@UniqueConstraint(name = "uk_workspace_key", columnNames = "workspace_key")})
@SQLRestriction("soft_deleted = false")
public class Workspace extends SoftDeleteEntity {

    private static final Pattern KEY_PATTERN = Pattern.compile(KEY_REGEX);

    @Column(name = "workspace_key", nullable = false)
    private String key;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description = "";

    @SuppressWarnings("NullAway.Init")
    protected Workspace() {}

    public static Workspace create(String key, String name, @Nullable String description) {
        validateKey(key);

        Workspace workspace = new Workspace();
        workspace.key = key.toUpperCase();
        workspace.name = name;
        workspace.description = Objects.requireNonNullElse(description, "");

        return workspace;
    }

    private static void validateKey(String key) {
        if (key.length() < KEY_MIN_LENGTH || key.length() > KEY_MAX_LENGTH) {
            throw new BadRequestException(WorkspaceErrorCode.INVALID_WORKSPACE_KEY_FORMAT);
        }

        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new BadRequestException(WorkspaceErrorCode.INVALID_WORKSPACE_KEY_FORMAT);
        }
    }

    public void transferOwnership(WorkspaceMember owner, WorkspaceMember newOwner) {
        ensureEditable();
        if (!owner.isOwner()) {
            throw new ForbiddenException(WorkspaceErrorCode.WORKSPACE_OWNERSHIP_REQUIRED);
        }
        owner.updateRole(ADMIN);
        newOwner.changeRoleToOwner();
    }

    public void updateName(String name) {
        ensureEditable();
        this.name = name;
    }

    public void updateDescription(@Nullable String description) {
        ensureEditable();
        this.description = Objects.requireNonNullElse(description, "");
    }

    public void ensureEditable() {
        if (this.isArchived()) {
            throw new WorkspaceArchivedException(key);
        }
    }
}
