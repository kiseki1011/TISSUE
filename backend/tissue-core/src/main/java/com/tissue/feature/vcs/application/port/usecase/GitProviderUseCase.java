package com.tissue.feature.vcs.application.port.usecase;

import com.tissue.feature.vcs.application.dto.GitPrDto;
import com.tissue.feature.vcs.application.dto.GitPushDto;

public interface GitProviderUseCase {

    void handlePushEvent(GitPushDto event);

    void handlePullRequest(GitPrDto event);
}
