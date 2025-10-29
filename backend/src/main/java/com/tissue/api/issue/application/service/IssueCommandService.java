package com.tissue.api.issue.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.util.Patchers;
import com.tissue.api.issue.application.dto.request.CreateIssueCommand;
import com.tissue.api.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateCustomFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateStoryPointCommand;
import com.tissue.api.issue.application.dto.response.IssueCommandResult;
import com.tissue.api.issue.application.finder.IssueFinder;
import com.tissue.api.issue.application.finder.IssueTypeFinder;
import com.tissue.api.issue.application.port.in.IssueCommandUseCase;
import com.tissue.api.issue.application.port.out.IssueFieldValueRepository;
import com.tissue.api.issue.application.port.out.IssueRepository;
import com.tissue.api.issue.application.validator.IssueFieldSchemaValidator;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueFieldValue;
import com.tissue.api.issue.domain.vo.IssueContent;
import com.tissue.api.issue.domain.vo.IssueParticipants;
import com.tissue.api.issue.domain.vo.IssueSchedule;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspacemember.application.finder.WorkspaceMemberFinder;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueCommandService implements IssueCommandUseCase {

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

	@Override
	public IssueCommandResult create(CreateIssueCommand cmd) {
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

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult updateCommonFields(UpdateCommonFieldsCommand cmd) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceKey());

		Patchers.apply(cmd.title(), issue::updateTitle);
		Patchers.apply(cmd.content(), issue::updateContent);
		Patchers.apply(cmd.summary(), issue::updateSummary);
		Patchers.apply(cmd.dueAt(), issue::updateDueAt);
		Patchers.apply(cmd.priority(), issue::updatePriority);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult updateStoryPoint(UpdateStoryPointCommand cmd) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceKey());

		issue.updateStoryPoint(cmd.storyPoint());

		// TODO: 이벤트 발행 후, 리스너에서 처리 고려
		if (issue.getParentIssue() != null) {
			issue.getParentIssue().recalculateEpicStoryPoint();
		}

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult updateCustomFields(UpdateCustomFieldsCommand cmd) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceKey());

		// TODO: 확실하게 커맨드 dto 또는 변환 과정에서 처리하면 로직을 제거해도 되지 않을까?
		if (cmd.customFields() == null || cmd.customFields().isEmpty()) {
			return IssueCommandResult.from(issue);
		}

		List<IssueFieldValue> updateValues = fieldSchemaValidator.validateAndApplyPatch(
			cmd.customFields(),
			issue
		);
		fieldValueRepository.saveAll(updateValues);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult assignParent(String workspaceKey, String issueKey, String parentIssueKey) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		Issue parent = issueFinder.findIssue(parentIssueKey, workspaceKey);

		issue.setParentIssue(parent);

		// TODO: 이벤트 발행 후, 리스너에서 처리 고려
		if (issue.getParentIssue() != null) {
			issue.getParentIssue().recalculateEpicStoryPoint();
		}

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult removeParent(String workspaceKey, String issueKey) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);

		issue.removeParentIssue();

		// TODO: 이벤트 발행 후, 리스너에서 처리 고려
		if (issue.getParentIssue() != null) {
			issue.getParentIssue().recalculateEpicStoryPoint();
		}

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult softDelete(String workspaceKey, String issueKey) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);

		issue.softDelete();

		return IssueCommandResult.from(issue);
	}
}
