package com.tissue.feature.vcs.application.port.usecase;

import com.tissue.feature.vcs.application.dto.response.VcsIntegrationDetail;
import com.tissue.feature.vcs.application.dto.response.VcsWebhookDeliverySummary;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectVcsQueryUseCase {

    VcsIntegrationDetail getIntegration(String projectKey, VcsProvider provider, Long actorMemberId);

    Page<VcsWebhookDeliverySummary> getDeliveries(
            String projectKey, VcsProvider provider, Pageable pageable, Long actorMemberId);
}
