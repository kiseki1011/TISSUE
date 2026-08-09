package com.tissue.feature.vcs.application.port.usecase;

import com.tissue.feature.vcs.application.dto.VcsEventResult;
import com.tissue.feature.vcs.domain.enums.VcsProvider;

/**
 * Turns one provider's raw webhook payload into domain actions. Implemented once per provider in its web
 * adapter, so the inbox can replay a stored delivery without knowing any provider's JSON shape.
 *
 * <p>Implementations throw only for transient failures that are worth retrying. Anything permanent
 * (unparseable payload, event type we do not act on) must come back as a skipped result.
 */
public interface VcsWebhookDispatcher {

    VcsProvider provider();

    VcsEventResult dispatch(String projectKey, String eventType, String rawPayload);
}
