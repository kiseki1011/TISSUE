package com.tissue.team.domain;

import com.tissue.common.enums.ColorType;
import com.tissue.global.entity.BaseEntity;
import com.tissue.global.vo.Name;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMemberTeam;
import com.tissue.workspace.domain.exception.WorkspaceArchivedException;
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
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_workspace_team_name",
                    columnNames = {"workspace_id", "team_name_norm"})
        })
public class Team extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "team_name", nullable = false, length = 64)),
        @AttributeOverride(
                name = "normalized",
                column = @Column(name = "team_name_norm", nullable = false, length = 64))
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

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkspaceMemberTeam> workspaceMemberTeams = new ArrayList<>();

    @SuppressWarnings("NullAway.Init")
    protected Team() {}

    public static Team create(Workspace workspace, String name, @Nullable String description, ColorType color) {
        Team team = new Team();
        team.workspace = workspace;
        team.ensureEditable();
        team.workspaceKey = workspace.getKey();
        team.name = Name.of(name);
        team.description = description;
        team.color = color;

        return team;
    }

    public void updateName(String name) {
        ensureEditable();
        this.name = Name.of(name);
    }

    public void updateDescription(@Nullable String description) {
        ensureEditable();
        this.description = description;
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
