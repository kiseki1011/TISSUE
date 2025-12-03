package com.tissue.api.workspace.application.service.query;

import org.springframework.stereotype.Service;

import com.tissue.api.workspace.application.dto.response.query.WorkspaceDetail;
import com.tissue.api.workspace.application.port.in.WorkspaceQueryUseCase;
import com.tissue.api.workspace.application.port.out.WorkspaceQueryRepository;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.exception.WorkspaceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceQueryService implements WorkspaceQueryUseCase {

	private final WorkspaceQueryRepository workspaceQueryRepository;

	public WorkspaceDetail getDetail(String workspaceKey) {

		// TODO: 못찾을 시 그냥 빈 내용을 반환 고려
		Workspace workspace = workspaceQueryRepository.findByKey(workspaceKey)
			.orElseThrow(() -> new WorkspaceNotFoundException(workspaceKey));

		return WorkspaceDetail.from(workspace);
	}
}
