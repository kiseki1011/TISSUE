-- ============================================
-- LLM GENERATED
-- model: claude-opus-4-8
-- evaluation: NOT_REVIEWED
-- ============================================
-- Hand-written constraints/indexes that Hibernate's ddl-auto cannot express.
--
-- Applied in the relevant integration tests via @Sql(BEFORE_TEST_CLASS), after Hibernate's
-- `ddl-auto: create` builds the schema. Production owns this DDL out-of-band until Flyway,
-- at which point it folds into the V1 baseline alongside fts.sql / trgm.sql.

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
