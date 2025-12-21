package com.tissue.workspace.application.service.query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.workspace.application.port.in.WorkspaceMemberQueryUseCase;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkspaceMemberQueryService implements WorkspaceMemberQueryUseCase {
}
