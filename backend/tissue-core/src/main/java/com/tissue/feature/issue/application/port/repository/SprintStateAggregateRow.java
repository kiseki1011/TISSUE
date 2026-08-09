package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.workflow.domain.enums.StateCategory;

/**
 * Per-state-category aggregate over the issues currently in a sprint's scope: how many issues are in the
 * category and their summed story points. Story points exclude EPIC issues, whose points are a rollup of
 * their STANDARD children and would otherwise be double-counted when both sit in the same sprint. Only
 * categories that have at least one issue are returned.
 */
public interface SprintStateAggregateRow {

    StateCategory getCategory();

    long getCount();

    long getStoryPoints();
}
