-- Configuring a transition's guards is a full replacement: the existing rows are cleared and the
-- submitted set is inserted. The replacement reuses the same (transition_id, execution_order) and
-- (transition_id, guard_type) values, so the inserts collide with rows that are logically already
-- gone -- Hibernate does not guarantee that the deletes reach the database before the inserts.
-- Editing an existing guard (say min_approvals 2 -> 1) therefore failed with a 23505.
--
-- Deferring the check to commit is how the sibling workflow constraints already handle exactly this:
-- uk_workflow_state_workflow_id_normalized_name, uk_workflow_transition_edge and
-- uk_workflow_transition_source_name are all DEFERRABLE INITIALLY DEFERRED (V1__baseline.sql).
-- These two were the only ones in the family left immediate. The constraints still hold at commit,
-- so a genuinely duplicated order or guard type is still rejected.
ALTER TABLE public.transition_guard_config
    DROP CONSTRAINT uk_guard_config_order,
    ADD CONSTRAINT uk_guard_config_order
        UNIQUE (transition_id, execution_order) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE public.transition_guard_config
    DROP CONSTRAINT uk_guard_config_type,
    ADD CONSTRAINT uk_guard_config_type
        UNIQUE (transition_id, guard_type) DEFERRABLE INITIALLY DEFERRED;
