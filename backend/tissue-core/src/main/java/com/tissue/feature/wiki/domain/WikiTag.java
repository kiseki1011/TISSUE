package com.tissue.feature.wiki.domain;

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
        name = "wiki_tag",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_wiki_tag_normalized_name",
                    columnNames = {"normalized_name"})
        })
public class WikiTag extends HardDeleteEntity {

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
    protected WikiTag() {}

    public static WikiTag create(Name name, @Nullable String description, ColorType color) {
        WikiTag tag = new WikiTag();
        tag.name = name;
        tag.description = Objects.requireNonNullElse(description, "");
        tag.color = color;
        return tag;
    }

    public String getName() {
        return name.toString();
    }
}
