package com.tissue.feature.vcs.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VcsProvider {
    GITHUB("/tree/"),
    GITLAB("/-/tree/"),
    GITEA("/src/branch/"),
    FORGEJO("/src/branch/");

    private final String branchPath;

    /**
     * Builds a web URL for a specific branch.
     *
     * @param repoUrl
     *         the base repository URL, for example
     *         {@code https://github.com/kiseki1011/TISSUE}
     * @param branchName
     *         the name of the branch, for example {@code main}
     *
     * @return the full web URL to the branch
     */
    public String buildBranchUrl(String repoUrl, String branchName) {
        return repoUrl + this.branchPath + branchName;
    }
}
