package com.tissue.vcs.application.port.in;

import com.tissue.vcs.domain.GitPrDto;
import com.tissue.vcs.domain.GitPushDto;

public interface GitProviderUseCase {

    void handlePullRequest(GitPrDto event);

    void handlePushEvent(GitPushDto event);
}
