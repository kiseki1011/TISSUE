package com.tissue.feature.agent.domain;

/**
 * Functional category an agent is registered for. Only agents carry a type; human members leave it
 * {@code null}. {@link #GENERAL} is the default when none is specified.
 */
public enum AgentType {
    DEVELOPMENT,
    PLANNING,
    MANAGEMENT,
    DESIGN,
    QA,
    GENERAL,
}
