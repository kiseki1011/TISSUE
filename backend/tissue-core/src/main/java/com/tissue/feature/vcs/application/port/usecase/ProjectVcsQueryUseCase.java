package com.tissue.feature.vcs.application.port.usecase;

import com.tissue.feature.vcs.application.dto.response.VcsIntegrationDetail;
import com.tissue.feature.vcs.domain.enums.VcsProvider;

public interface ProjectVcsQueryUseCase {

    VcsIntegrationDetail getIntegration(String projectKey, VcsProvider provider, Long actorMemberId);
}
