package com.tissue.feature.vcs.application.port.usecase;

import com.tissue.feature.vcs.application.dto.response.VcsSecretResponse;
import com.tissue.feature.vcs.domain.enums.VcsProvider;

public interface ProjectVcsCommandUseCase {

    VcsSecretResponse regenerateSecret(String projectKey, VcsProvider provider, Long actorMemberId);

    void removeIntegration(String projectKey, VcsProvider provider, Long actorMemberId);
}
