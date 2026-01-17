package com.tissue.notification.infrastructure.repository;

import com.tissue.notification.domain.FailedEmail;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface FailedEmailRepository extends Repository<FailedEmail, Long> {

    FailedEmail save(FailedEmail failedEmail);

    void delete(FailedEmail failedEmail);

    List<FailedEmail> findAllByNextRetryAtBefore(LocalDateTime now);
}
