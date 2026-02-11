package com.tissue.feature.vcs.application.port.usecase;

import com.tissue.feature.vcs.application.dto.GitPrDto;
import com.tissue.feature.vcs.application.dto.GitPushDto;

public interface GitProviderUseCase {

    void handlePullRequest(GitPrDto event);

    void handlePushEvent(GitPushDto event);
}
