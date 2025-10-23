package com.tissue.api.issue.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.util.Patchers;
import com.tissue.api.issue.application.dto.CreateIssueCommand;
import com.tissue.api.issue.application.dto.UpdateCommonFieldsCommand;
import com.tissue.api.issue.application.dto.UpdateCustomFieldsCommand;
import com.tissue.api.issue.application.dto.UpdateStoryPointCommand;
import com.tissue.api.issue.application.finder.IssueFinder;
import com.tissue.api.issue.application.finder.IssueTypeFinder;
import com.tissue.api.issue.application.validator.IssueFieldSchemaValidator;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.IssueFieldValue;
import com.tissue.api.issue.domain.model.vo.IssueContent;
import com.tissue.api.issue.domain.model.vo.IssueParticipants;
import com.tissue.api.issue.domain.model.vo.IssueSchedule;
import com.tissue.api.issue.infrastructure.repository.IssueFieldValueRepository;
import com.tissue.api.issue.infrastructure.repository.IssueRepository;
import com.tissue.api.issue.presentation.dto.response.IssueResponse;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberFinder;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueService {

	private final IssueFinder issueFinder;
	private final IssueTypeFinder issueTypeFinder;
	private final WorkspaceFinder workspaceFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final IssueFieldSchemaValidator fieldSchemaValidator;

	private final IssueRepository issueRepository;
	private final IssueFieldValueRepository fieldValueRepository;

	/**
	 * TODO(later)
	 *  (추후에 Workspace -> Project로 리팩토링 후 진행) 컨트롤러에서의 authorization외에도 assignee 또는 author만 수정을 가능하도록 토글 할 수 있는 권한 체계 고려.
	 *  이때 ProjectRole(구 WorkspaceRole)은 ADMIN 이상이면 무조건 수정 가능해야 함
	 */

	@Transactional
	public IssueResponse create(CreateIssueCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findIssueType(workspace, cmd.issueTypeId());
		WorkspaceMember actor = workspaceMemberFinder.findWorkspaceMember(cmd.memberId(), cmd.workspaceKey());

		Issue issue = issueRepository.save(Issue.create(
			workspace,
			issueType,
			cmd.title(),
			IssueContent.of(cmd.content(), cmd.summary()),
			IssueSchedule.of(cmd.dueAt()),
			IssueParticipants.of(actor),
			cmd.priority(),
			cmd.storyPoint()
		));

		List<IssueFieldValue> values = fieldSchemaValidator.validateAndExtract(cmd.customFields(), issue);
		fieldValueRepository.saveAll(values);

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse updateCommonFields(UpdateCommonFieldsCommand cmd) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceKey());

		Patchers.apply(cmd.title(), issue::updateTitle);
		Patchers.apply(cmd.content(), issue::updateContent);
		Patchers.apply(cmd.summary(), issue::updateSummary);
		Patchers.apply(cmd.dueAt(), issue::updateDueAt);
		Patchers.apply(cmd.priority(), issue::updatePriority);

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse updateStoryPoint(UpdateStoryPointCommand cmd) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceKey());

		issue.updateStoryPoint(cmd.storyPoint());

		// TODO: 이벤트 발행 후, 리스너에서 처리 고려
		if (issue.getParentIssue() != null) {
			issue.getParentIssue().recalculateEpicStoryPoint();
		}

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse updateCustomFields(UpdateCustomFieldsCommand cmd) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceKey());

		// TODO: 확실하게 커맨드 dto 또는 변환 과정에서 처리하면 로직을 제거해도 되지 않을까?
		if (cmd.customFields() == null || cmd.customFields().isEmpty()) {
			return IssueResponse.from(issue);
		}

		List<IssueFieldValue> updateValues = fieldSchemaValidator.validateAndApplyPatch(
			cmd.customFields(),
			issue
		);
		fieldValueRepository.saveAll(updateValues);

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse assignParent(String workspaceKey, String issueKey, String parentIssueKey) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		Issue parent = issueFinder.findIssue(parentIssueKey, workspaceKey);

		issue.setParentIssue(parent);

		// TODO: 이벤트 발행 후, 리스너에서 처리 고려
		if (issue.getParentIssue() != null) {
			issue.getParentIssue().recalculateEpicStoryPoint();
		}

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse removeParent(String workspaceKey, String issueKey) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);

		issue.removeParentIssue();

		// TODO: 이벤트 발행 후, 리스너에서 처리 고려
		if (issue.getParentIssue() != null) {
			issue.getParentIssue().recalculateEpicStoryPoint();
		}

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse softDelete(String workspaceKey, String issueKey) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);

		issue.softDelete();

		return IssueResponse.from(issue);
	}

	// TODO: requestReview()
	// TODO: batchChangeParent()
	// TODO: batchUpdateStoryPoint()
	// TODO: batchSoftDelete()
	// TODO: cloneIssue()
	// TODO: cloneIssueToProject()
}
