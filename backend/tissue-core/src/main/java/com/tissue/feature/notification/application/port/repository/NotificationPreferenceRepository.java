package com.tissue.feature.notification.application.port.repository;

import com.tissue.feature.notification.domain.NotificationPreference;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface NotificationPreferenceRepository extends Repository<NotificationPreference, Long> {

    NotificationPreference save(NotificationPreference preference);

    Optional<NotificationPreference> findByReceiverMemberIdAndWorkspaceKey(Long memberId, String workspaceKey);

    List<NotificationPreference> findAllByWorkspaceKeyAndReceiverMemberIdIn(
            String workspaceKey, Collection<Long> memberIds);
}
