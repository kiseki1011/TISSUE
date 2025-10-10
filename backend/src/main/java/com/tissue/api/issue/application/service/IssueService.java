package com.tissue.api.issue.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.util.Patchers;
import com.tissue.api.issue.application.dto.CreateIssueCommand;
import com.tissue.api.issue.application.dto.UpdateCommonFieldsCommand;
import com.tissue.api.issue.application.dto.UpdateCustomFieldsCommand;
import com.tissue.api.issue.application.finder.IssueFinder;
import com.tissue.api.issue.application.finder.IssueTypeFinder;
import com.tissue.api.issue.application.validator.IssueFieldSchemaValidator;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.IssueFieldValue;
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
	 *  1. (추후에 Workspace -> Project로 리팩토링 후 진행) 컨트롤러에서의 authorization외에도 assignee 또는 author만 수정을 가능하도록 토글 할 수 있는 권한 체계 고려.
	 *  이때 ProjectRole(구 WorkspaceRole)은 ADMIN 이상이면 무조건 수정 가능해야 함
	 *  2. 이슈를 클론하는 API 구현. (근데 이 경우에는 IssueRelation.CLONE을 설정해줘야 할까?)
	 *  3. 특정 부모 이슈 아래의 자식 이슈들을 다른 부모 이슈 아래로 배치(batch)로 이동 시키는 API 구현.
	 *  각 이동되는 이슈마다 이동이 가능한지 검증해줘야 함. (issue.ensureCanAddParent 사용?)
	 */

	@Transactional
	public IssueResponse create(CreateIssueCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findIssueType(workspace, cmd.issueTypeId());
		WorkspaceMember actor = workspaceMemberFinder.findWorkspaceMember(cmd.currentMemberId(), cmd.workspaceKey());

		Issue issue = issueRepository.save(Issue.create(
			workspace,
			issueType,
			cmd.title(),
			cmd.content(),
			cmd.summary(),
			cmd.priority(),
			cmd.dueAt(),
			cmd.storyPoint()
		));

		List<IssueFieldValue> values = fieldSchemaValidator.validateAndExtract(cmd.customFields(), issue);
		fieldValueRepository.saveAll(values);

		// TODO: 아래 로직을 Issue.create에 캡슐화 하는게 좋을까?
		issue.updateReporter(actor); // TODO: updateReporter 대신 setReporter가 더 나으려나?
		issue.addSubscriber(actor);

		return IssueResponse.from(issue);
	}

	// TODO: updateReporter도 여기서 업데이트? 아니면 따로 API를 분리할까?
	@Transactional
	public IssueResponse updateCommonFields(UpdateCommonFieldsCommand cmd) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceKey());

		Patchers.apply(cmd.title(), issue::updateTitle);
		Patchers.apply(cmd.content(), issue::updateContent);
		Patchers.apply(cmd.summary(), issue::updateSummary);
		Patchers.apply(cmd.dueAt(), issue::updateDueAt);
		Patchers.apply(cmd.priority(), issue::updatePriority);
		Patchers.apply(cmd.storyPoint(), issue::updateStoryPoint);

		return IssueResponse.from(issue);
	}

	// TODO: 가독성 리팩토링 가능할까?
	//  early-return을 사용하면 더 좋을까? 아니면 조건문의 조건을 변수로 추출해서 이름을 붙일가?
	@Transactional
	public IssueResponse updateCustomFields(UpdateCustomFieldsCommand cmd) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceKey());

		if (cmd.customFields() != null && !cmd.customFields().isEmpty()) {
			List<IssueFieldValue> updateValues = fieldSchemaValidator.validateAndApplyPatch(
				cmd.customFields(),
				issue
			);
			fieldValueRepository.saveAll(updateValues);
		}

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse assignParent(String workspaceKey, String issueKey, String parentIssueKey) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		Issue parent = issueFinder.findIssue(parentIssueKey, workspaceKey);

		issue.assignParentIssue(parent);

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse removeParent(String workspaceKey, String issueKey) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);

		issue.removeParentIssue();

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse softDelete(String workspaceKey, String issueKey) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);

		// issueValidator.ensureDeletable();
		issue.softDelete();

		return IssueResponse.from(issue);
	}

	// TODO: requestReview()
}
