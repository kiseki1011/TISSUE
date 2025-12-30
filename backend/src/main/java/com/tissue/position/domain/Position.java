package com.tissue.position.domain;

import com.tissue.common.entity.BaseEntity;
import com.tissue.common.enums.ColorType;
import com.tissue.common.vo.Name;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMemberPosition;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

// TODO: should position use soft-delete or hard-delete?
//  current policy: check if anybody uses it, if nobody uses, then hard-delete
@Entity
@Getter
@Table(
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_workspace_position_name",
                    columnNames = {"workspace_id", "position_name_norm"})
        })
public class Position extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "position_name", nullable = false, length = 64)),
        @AttributeOverride(
                name = "normalized",
                column = @Column(name = "position_name_norm", nullable = false, length = 64))
    })
    private Name name;

    @Nullable
    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false)
    private ColorType color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "workspace_key", nullable = false)
    private String workspaceKey;

    @OneToMany(mappedBy = "position", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkspaceMemberPosition> workspaceMemberPositions = new ArrayList<>();

    @SuppressWarnings("NullAway.Init")
    protected Position() {}

    @Builder
    public Position(Workspace workspace, String name, @Nullable String description, ColorType color) {
        this.workspace = workspace;
        this.workspaceKey = workspace.getKey();
        this.name = Name.of(name);
        this.description = description;
        this.color = color;
    }

    public void updateName(String name) {
        this.name = Name.of(name);
    }

    public void updateDescription(@Nullable String description) {
        this.description = description;
    }

    public void updateColor(ColorType color) {
        this.color = color;
    }

    public String getDisplayName() {
        return name.getDisplay();
    }
}
