package com.tissue.feature.workflow.domain;

import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.entity.HardDeleteEntity;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
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
import jakarta.persistence.Version;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
public class WorkflowState extends HardDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;

    @Embedded
    private Name name;

    @Column(name = "description")
    private String description = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false)
    private ColorType color;

    @Enumerated(EnumType.STRING)
    @Column(name = "state_category", nullable = false)
    private StateCategory category;

    @SuppressWarnings("NullAway.Init")
    protected WorkflowState() {}

    static WorkflowState of(Name name, @Nullable String description, ColorType color, StateCategory category) {
        WorkflowState ws = new WorkflowState();
        ws.name = name;
        ws.description = Objects.requireNonNullElse(description, "");
        ws.color = color;
        ws.category = category;
        return ws;
    }

    void attachToWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    void updateName(Name name) {
        this.name = name;
    }

    public void updateDescription(@Nullable String description) {
        this.description = Objects.requireNonNullElse(description, "");
    }

    public void updateColor(ColorType color) {
        this.color = color;
    }

    void categorizeAs(StateCategory category) {
        this.category = category;
    }

    public String getDisplayName() {
        return name.getDisplay();
    }

    public boolean isCategorizedAs(StateCategory category) {
        return getCategory() == category;
    }
}
