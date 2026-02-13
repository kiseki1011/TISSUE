package com.tissue.shared.dto;

public record ProjectIdentifier(String workspaceKey, String projectKey) {

    public static ProjectIdentifier of(String workspaceKey, String projectKey) {
        return new ProjectIdentifier(workspaceKey, projectKey);
    }
}
