package com.tissue.workspace.domain;

import static com.tissue.workspace.domain.enums.WorkspaceRole.*;

import com.tissue.common.entity.BaseEntity;
import com.tissue.workspace.domain.exception.WorkspaceExceptions;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

@Entity
@SQLRestriction("softDeleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workspace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workspace_id")
    private Long id;

    @Column(name = "workspace_key", unique = true, nullable = false)
    private String key;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    // TODO: consider adding a icon field

    public static Workspace create(
            @NonNull String key, @NonNull String name, @Nullable String description) {
        Workspace workspace = new Workspace();
        workspace.key = key;
        workspace.name = name;
        workspace.description = description;

        return workspace;
    }

    // TODO: should i separate this into a separate domain service?
    public void transferOwnership(
            @NonNull WorkspaceMember owner, @NonNull WorkspaceMember newOwner) {
        if (!owner.isOwner()) {
            throw WorkspaceExceptions.ownershipRequired(owner);
        }
        owner.changeRoleTo(ADMIN);
        newOwner.changeRoleToOwner();
    }

    public void updateName(@NonNull String name) {
        this.name = name;
    }

    public void updateDescription(@Nullable String description) {
        this.description = description;
    }
}
