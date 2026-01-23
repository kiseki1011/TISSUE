package com.tissue.vcs.application.port.in;

import com.tissue.vcs.domain.GitPrDto;

public interface GitProviderUseCase {
    void handlePullRequest(GitPrDto event);
}
