package com.tissue.feature.activitylog.application.service;

import com.tissue.feature.activitylog.application.dto.request.CreateLogCommand;
import com.tissue.feature.activitylog.application.dto.request.CreateLogWithDiffCommand;
import com.tissue.feature.activitylog.application.port.repository.ActivityLogCommandRepository;
import com.tissue.feature.activitylog.domain.ActivityLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ActivityLogCommandService {

    private final ActivityLogCommandRepository repository;

    public void createLogWithDiff(CreateLogWithDiffCommand cmd) {
        ActivityLog log = ActivityLog.builder()
                .eventId(cmd.eventId())
                .activityType(cmd.activityType())
                .entityReference(cmd.reference())
                .actorMemberId(cmd.actorMemberId())
                .data(cmd.data())
                .changes(cmd.changes())
                .build();

        repository.save(log);
    }

    public void createLog(CreateLogCommand cmd) {
        ActivityLog log = ActivityLog.builder()
                .eventId(cmd.eventId())
                .activityType(cmd.activityType())
                .entityReference(cmd.reference())
                .actorMemberId(cmd.actorMemberId())
                .data(cmd.data())
                .build();

        repository.save(log);
    }
}
