package com.tissue.workspace.domain;

import static com.tissue.workspace.domain.enums.WorkspaceRole.ADMIN;

import com.tissue.global.entity.SoftDeleteEntity;
import com.tissue.workspace.domain.exception.WorkspaceArchivedException;
import com.tissue.workspace.domain.exception.WorkspaceOwnershipRequiredException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Objects;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@SQLRestriction("soft_deleted = false")
public class Workspace extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workspace_id")
    private Long id;

    @Column(name = "workspace_key", unique = true, nullable = false)
    private String key;

    @Column(name = "name", nullable = false)
    private String name;

    @Nullable
    @Column(name = "description")
    private String description;

    @SuppressWarnings("NullAway.Init")
    protected Workspace() {}

    public static Workspace create(String key, String name, @Nullable String description) {
        Workspace workspace = new Workspace();
        workspace.key = key;
        workspace.name = name;
        workspace.description = Objects.requireNonNullElse(description, "");

        return workspace;
    }

    public void transferOwnership(WorkspaceMember owner, WorkspaceMember newOwner) {
        ensureEditable();
        if (!owner.isOwner()) {
            throw new WorkspaceOwnershipRequiredException(owner);
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
