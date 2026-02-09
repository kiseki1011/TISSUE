package com.tissue.vcs.application.port.in;

import com.tissue.vcs.application.dto.GitPrDto;
import com.tissue.vcs.application.dto.GitPushDto;

public interface GitProviderUseCase {

    void handlePullRequest(GitPrDto event);

    void handlePushEvent(GitPushDto event);
}
