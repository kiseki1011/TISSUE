package com.tissue.feature.vcs.application.service;

import com.tissue.feature.vcs.domain.enums.VcsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Front door of the webhook inbox: stores the delivery, then hands it off for asynchronous processing.
 *
 * <p>Storing first is what makes the endpoint safe to answer immediately. A provider that redelivers the
 * same event hits the unique delivery id and is dropped here rather than replaying a state transition,
 * and a delivery that fails downstream is still on disk to retry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VcsWebhookInboxService {

    private final VcsWebhookDeliveryWriter writer;
    private final VcsWebhookProcessor processor;

    /**
     * Stores the delivery and queues it for processing.
     *
     * @return false when this delivery was already received and was therefore dropped
     */
    public boolean receive(
            VcsProvider provider, String deliveryId, String projectKey, String eventType, String payload) {
        Long id;
        try {
            id = writer.create(provider, deliveryId, projectKey, eventType, payload);
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate {} webhook delivery {} dropped for project {}", provider, deliveryId, projectKey);
            return false;
        }

        processor.processAsync(id);
        return true;
    }
}
