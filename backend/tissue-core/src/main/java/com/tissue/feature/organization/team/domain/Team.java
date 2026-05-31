package com.tissue.feature.organization.team.domain;

import com.tissue.shared.entity.HardDeleteEntity;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(
        name = "team",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_team_name",
                    columnNames = {"normalized_name"})
        })
public class Team extends HardDeleteEntity {

    @Version
    private Long version;

    @Embedded
    private Name name;

    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false)
    private ColorType color;

    @SuppressWarnings("NullAway.Init")
    protected Team() {}

    public static Team create(Name name, @Nullable String description, ColorType color) {
        Team team = new Team();
        team.name = name;
        team.description = Objects.requireNonNullElse(description, "");
        team.color = color;

        return team;
    }

    public String getName() {
        return name.toString();
    }

    public void rename(Name name) {
        this.name = name;
    }

    public void updateDescription(@Nullable String description) {
        this.description = Objects.requireNonNullElse(description, "");
    }

    public void updateColor(ColorType color) {
        this.color = color;
    }
}
