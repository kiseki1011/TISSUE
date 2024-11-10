package com.uranus.taskmanager.api.workspace.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.security.PasswordEncoder;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.WorkspaceUpdateDetail;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceContentUpdateRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceDeleteRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspacePasswordUpdateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceContentUpdateResponse;
import com.uranus.taskmanager.api.workspace.exception.InvalidWorkspacePasswordException;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspacemember.exception.MemberNotInWorkspaceException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class WorkspaceCommandService {

	private final WorkspaceRepository workspaceRepository;
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public WorkspaceContentUpdateResponse updateWorkspaceContent(WorkspaceContentUpdateRequest request, String code) {
		Workspace workspace = findWorkspaceByCode(code);
		WorkspaceUpdateDetail original = WorkspaceUpdateDetail.from(workspace);

		if (request.hasName()) {
			workspace.updateName(request.getName());
		}
		if (request.hasDescription()) {
			workspace.updateDescription(request.getDescription());
		}

		WorkspaceUpdateDetail updatedTo = WorkspaceUpdateDetail.from(workspace);
		return WorkspaceContentUpdateResponse.from(original, updatedTo);
	}

	@Transactional
	public void updateWorkspacePassword(WorkspacePasswordUpdateRequest request, String code) {
		Workspace workspace = findWorkspaceByCode(code);

		validatePasswordIfExists(workspace.getPassword(), request.getOriginalPassword());

		String encodedUpdatePassword = encodePasswordIfPresent(request.getUpdatePassword());
		workspace.updatePassword(encodedUpdatePassword);
	}

	@Transactional
	public void deleteWorkspace(WorkspaceDeleteRequest request, String code) {
		Workspace workspace = findWorkspaceByCode(code);
		/*
		 * Todo
		 *  - 워크스페이스의 주인(생성자)의 로그인 Id를 통해서 주인 멤버 찾기
		 *  - 해당 멤버의 workspaceCount 감소
		 *  - 추후에 로직 개선 필요(DD를 적용하면 전체적으로 변할 듯)
		 *  - 주인(Owner) Role 추가 -> 그냥 워크스페이스 삭제를 주인만 가능하도록 제한하면 더 쉬울 듯
		 *  - -> 컨트롤러에서 LoginMember(id)를 읽어와서 사용하면 됨
		 */
		Member workspaceOwner = memberRepository.findByLoginId(workspace.getCreatedBy())
			.orElseThrow(MemberNotInWorkspaceException::new);

		workspaceOwner.decreaseWorkspaceCount();

		validatePasswordIfExists(workspace.getPassword(), request.getPassword());

		workspaceRepository.delete(workspace);
	}

	private Workspace findWorkspaceByCode(String workspaceCode) {
		return workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);
	}

	private String encodePasswordIfPresent(String password) {
		return Optional.ofNullable(password)
			.map(passwordEncoder::encode)
			.orElse(null);
	}

	private void validatePasswordIfExists(String workspacePassword, String inputPassword) {
		if (workspacePassword == null) {
			return;
		}
		if (passwordDoesNotMatch(workspacePassword, inputPassword)) {
			throw new InvalidWorkspacePasswordException();
		}
	}

	private boolean passwordDoesNotMatch(String workspacePassword, String inputPassword) {
		return !passwordEncoder.matches(inputPassword, workspacePassword);
	}
}
