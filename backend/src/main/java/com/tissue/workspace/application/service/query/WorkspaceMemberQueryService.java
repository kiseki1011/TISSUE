package com.tissue.workspace.application.service.query;

import com.tissue.workspace.application.port.in.WorkspaceMemberQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkspaceMemberQueryService implements WorkspaceMemberQueryUseCase {}
