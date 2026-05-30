package com.tissue.shared.dto;

public record ProjectIdentifier(String projectKey) {

    /**
     * Builds an identifier from a globally-unique projectKey. Used by the {@code /api/v1/projects/...} URLs.
     */
    public static ProjectIdentifier ofProjectKey(String projectKey) {
        return new ProjectIdentifier(projectKey);
    }
}
