package com.tissue.feature.organization.position.domain;

import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMemberPosition;
import com.tissue.feature.workspace.domain.exception.WorkspaceArchivedException;
import com.tissue.shared.entity.HardDeleteEntity;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_workspace_position_name",
                    columnNames = {"workspace_id", "position_name_norm"})
        })
public class Position extends HardDeleteEntity {

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "position_name", nullable = false, length = 64)),
        @AttributeOverride(
                name = "normalized",
                column = @Column(name = "position_name_norm", nullable = false, length = 64))
    })
    private Name name;

    @Column(name = "description", length = 255)
    private String description = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false)
    private ColorType color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "workspace_key", nullable = false, updatable = false)
    private String workspaceKey;

    @OneToMany(mappedBy = "position", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkspaceMemberPosition> workspaceMemberPositions = new ArrayList<>();

    @SuppressWarnings("NullAway.Init")
    protected Position() {}

    public static Position create(Workspace workspace, String name, @Nullable String description, ColorType color) {
        Position position = new Position();
        position.workspace = workspace;
        position.ensureEditable();
        position.workspaceKey = workspace.getKey();
        position.name = Name.of(name);
        position.description = Objects.requireNonNullElse(description, "");
        position.color = color;

        return position;
    }

    public void updateName(String name) {
        ensureEditable();
        this.name = Name.of(name);
    }

    public void updateDescription(@Nullable String description) {
        ensureEditable();
        this.description = Objects.requireNonNullElse(description, "");
    }

    public void updateColor(ColorType color) {
        ensureEditable();
        this.color = color;
    }

    public void ensureEditable() {
        if (workspace.isArchived()) {
            throw new WorkspaceArchivedException(workspaceKey);
        }
    }
}
