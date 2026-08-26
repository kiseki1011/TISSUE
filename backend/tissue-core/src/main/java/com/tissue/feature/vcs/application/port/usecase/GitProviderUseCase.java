package com.tissue.feature.vcs.application.port.usecase;

import com.tissue.feature.vcs.application.dto.GitPrDto;
import com.tissue.feature.vcs.application.dto.GitPushDto;
import com.tissue.feature.vcs.application.dto.VcsEventResult;

public interface GitProviderUseCase {

    VcsEventResult handlePushEvent(GitPushDto event);

    VcsEventResult handlePullRequest(GitPrDto event);
}
