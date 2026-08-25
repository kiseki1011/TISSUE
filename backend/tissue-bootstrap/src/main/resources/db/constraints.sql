-- ============================================
-- LLM GENERATED
-- model: claude-opus-4-8
-- evaluation: NOT_REVIEWED
-- ============================================
-- Hand-written constraints/indexes that Hibernate's ddl-auto cannot express.
--
-- Flyway now owns production and test schemas (V1__baseline.sql folded this file in). What still reads
-- it is the loadtest profile, which builds from `ddl-auto: create`, and extract-schema.sh, which
-- regenerates the baseline. So this stays the source of truth: a constraint changed by a migration has
-- to be mirrored here, or the next baseline regeneration silently reverts it.

-- Sprint: at most one ACTIVE (non-deleted) sprint per project.
-- A partial unique index — cannot be expressed as a JPA @UniqueConstraint.
CREATE UNIQUE INDEX IF NOT EXISTS uk_sprint_active_per_project
    ON sprint (project_id)
    WHERE sprint_status = 'ACTIVE' AND soft_deleted = false;

-- Workflow graph (state/transition) uniqueness is DEFERRABLE INITIALLY DEFERRED so it is checked
-- at COMMIT, not per row. A single WorkflowGraphReplaceService transaction may delete+recreate a
-- same-named node or swap edges; Hibernate flushes inserts before deletes, so an *immediate* unique
-- constraint would fail on the transient duplicate. The final graph is still guaranteed unique.

ALTER TABLE workflow_state
    DROP CONSTRAINT IF EXISTS uk_workflow_state_workflow_id_normalized_name;
ALTER TABLE workflow_state
    ADD CONSTRAINT uk_workflow_state_workflow_id_normalized_name
    UNIQUE (workflow_id, normalized_name) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE workflow_transition
    DROP CONSTRAINT IF EXISTS uk_workflow_transition_edge;
ALTER TABLE workflow_transition
    ADD CONSTRAINT uk_workflow_transition_edge
    UNIQUE (workflow_id, source_state_id, target_state_id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE workflow_transition
    DROP CONSTRAINT IF EXISTS uk_workflow_transition_source_name;
ALTER TABLE workflow_transition
    ADD CONSTRAINT uk_workflow_transition_source_name
    UNIQUE (source_state_id, normalized_name) DEFERRABLE INITIALLY DEFERRED;

-- Transition guards are deferred for the same reason, one level down. Configuring a transition's
-- guards replaces the whole set, re-submitting the same orders and guard types, so the inserts meet
-- rows that are logically already gone. Both constraints need it: deferring only the order one moves
-- the violation to the type one on the very next save. See V8__defer_transition_guard_config_constraints.sql.

ALTER TABLE transition_guard_config
    DROP CONSTRAINT IF EXISTS uk_guard_config_order;
ALTER TABLE transition_guard_config
    ADD CONSTRAINT uk_guard_config_order
    UNIQUE (transition_id, execution_order) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE transition_guard_config
    DROP CONSTRAINT IF EXISTS uk_guard_config_type;
ALTER TABLE transition_guard_config
    ADD CONSTRAINT uk_guard_config_type
    UNIQUE (transition_id, guard_type) DEFERRABLE INITIALLY DEFERRED;
