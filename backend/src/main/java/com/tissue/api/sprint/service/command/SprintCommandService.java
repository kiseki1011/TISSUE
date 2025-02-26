package com.tissue.api.sprint.service.command;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.service.query.IssueQueryService;
import com.tissue.api.sprint.domain.Sprint;
import com.tissue.api.sprint.domain.repository.SprintRepository;
import com.tissue.api.sprint.presentation.dto.request.AddSprintIssuesRequest;
import com.tissue.api.sprint.presentation.dto.request.CreateSprintRequest;
import com.tissue.api.sprint.presentation.dto.request.RemoveSprintIssueRequest;
import com.tissue.api.sprint.presentation.dto.request.UpdateSprintContentRequest;
import com.tissue.api.sprint.presentation.dto.request.UpdateSprintDateRequest;
import com.tissue.api.sprint.presentation.dto.request.UpdateSprintStatusRequest;
import com.tissue.api.sprint.presentation.dto.response.AddSprintIssuesResponse;
import com.tissue.api.sprint.presentation.dto.response.CreateSprintResponse;
import com.tissue.api.sprint.presentation.dto.response.UpdateSprintContentResponse;
import com.tissue.api.sprint.presentation.dto.response.UpdateSprintDateResponse;
import com.tissue.api.sprint.presentation.dto.response.UpdateSprintStatusResponse;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.service.query.WorkspaceQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SprintCommandService {

	private final SprintReader sprintReader;
	private final SprintRepository sprintRepository;
	private final WorkspaceQueryService workspaceQueryService;
	private final IssueQueryService issueQueryService;

	@Transactional
	public CreateSprintResponse createSprint(
		String workspaceCode,
		CreateSprintRequest request
	) {
		Workspace workspace = workspaceQueryService.findWorkspace(workspaceCode);

		Sprint sprint = Sprint.builder()
			.title(request.title())
			.goal(request.goal())
			.startDate(request.startDate())
			.endDate(request.endDate())
			.workspace(workspace)
			.build();

		Sprint savedSprint = sprintRepository.save(sprint);
		return CreateSprintResponse.from(savedSprint);
	}

	@Transactional
	public AddSprintIssuesResponse addIssues(
		String workspaceCode,
		String sprintKey,
		AddSprintIssuesRequest request
	) {
		Sprint sprint = sprintReader.findSprint(sprintKey, workspaceCode);

		List<Issue> issues = issueQueryService.findIssues(request.issueKeys(), workspaceCode);

		for (Issue issue : issues) {
			sprint.addIssue(issue);
		}

		return AddSprintIssuesResponse.of(sprint, request.issueKeys());
	}

	@Transactional
	public UpdateSprintContentResponse updateSprintContent(
		String workspaceCode,
		String sprintKey,
		UpdateSprintContentRequest request
	) {
		Sprint sprint = sprintReader.findSprint(sprintKey, workspaceCode);

		sprint.updateTitle(request.title() != null ? request.title() : sprint.getTitle());
		sprint.updateGoal(request.goal() != null ? request.goal() : sprint.getGoal());

		return UpdateSprintContentResponse.from(sprint);
	}

	@Transactional
	public UpdateSprintDateResponse updateSprintDate(
		String workspaceCode,
		String sprintKey,
		UpdateSprintDateRequest request
	) {
		Sprint sprint = sprintReader.findSprint(sprintKey, workspaceCode);

		/*
		 * Todo
		 *  - NotNull 검증을 컨트롤러단에서 처리하는데, 굳이 null에 대한 처리는 하지 않아도 되나?
		 *  - 그런데 또 나중에 API 계약이 변경되는 경우를 생각해서 null에 관한 처리를 하도록 구현하는게 좋을까?
		 *  - 서비스 재사용 그리고 변경 최소화의 관점에서는 null 처리하는게 좋을 것 같기도 하고...
		 *  - 그런데 null인 경우는 업데이트하지 않도록 구현해버리면, 의도된 null인지, 문제가 있어서 null이 들어온건지 판단은 어떻게 하지?
		 */
		LocalDate startDate = request.startDate() != null ? request.startDate() : sprint.getStartDate();
		LocalDate endDate = request.endDate() != null ? request.endDate() : sprint.getEndDate();

		if (request.startDate() != null || request.endDate() != null) {
			sprint.updateDates(startDate, endDate);
		}

		return UpdateSprintDateResponse.from(sprint);
	}

	@Transactional
	public UpdateSprintStatusResponse updateSprintStatus(
		String workspaceCode,
		String sprintKey,
		UpdateSprintStatusRequest request
	) {
		Sprint sprint = sprintReader.findSprint(sprintKey, workspaceCode);

		sprint.updateStatus(request.newStatus());

		return UpdateSprintStatusResponse.from(sprint);
	}

	@Transactional
	public void removeIssue(
		String workspaceCode,
		String sprintKey,
		RemoveSprintIssueRequest request
	) {
		Issue issue = issueQueryService.findIssueInSprint(sprintKey, request.issueKey(), workspaceCode);
		Sprint sprint = sprintReader.findSprint(sprintKey, workspaceCode);

		/*
		 * Todo: 성능 측정
		 *  - stream을 통해 메모리에 올려서 조회 vs 레포지토리 메서드(N+1 해결) vs 레포지토리 메서드
		 */
		// Issue issue = sprint.getSprintIssues().stream()
		// 	.filter(i -> request.issueKey().equals(i.getIssue().getIssueKey()))
		// 	.findFirst()
		// 	.orElseThrow(() -> new IssueNotFoundException(request.issueKey()))
		// 	.getIssue();

		sprint.removeIssue(issue);
	}
}
