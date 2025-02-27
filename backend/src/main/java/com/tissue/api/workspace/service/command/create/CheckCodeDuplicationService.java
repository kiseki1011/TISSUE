package com.tissue.api.workspace.service.command.create;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.InternalServerException;
import com.tissue.api.member.domain.Member;
import com.tissue.api.member.service.query.MemberQueryService;
import com.tissue.api.security.PasswordEncoder;
import com.tissue.api.util.RandomNicknameGenerator;
import com.tissue.api.util.WorkspaceCodeGenerator;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.presentation.dto.response.CreateWorkspaceResponse;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceCode의 중복 검사를 진행해서, 중복인 경우 재생성한다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckCodeDuplicationService implements WorkspaceCreateService {
	private static final int MAX_RETRIES = 5;

	private final MemberQueryService memberQueryService;
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceCodeGenerator workspaceCodeGenerator;
	private final RandomNicknameGenerator randomNicknameGenerator;
	private final PasswordEncoder passwordEncoder;

	/**
	 * 로그인 정보를 사용해서 멤버의 존재 유무를 검증한다
	 * 워크스페이스 생성 요청에 유일한 코드를 생성하고 설정한다
	 * 만약 코드가 중복되면 최대 5회 재성성 시도를 한다
	 * 워크스페이스 생성 요청을 사용해서 워크스페이스를 생성하고 저장한다
	 * 로그인 정보를 통해 찾은 멤버를 해당 워크스페이스에 참여시킨다
	 *
	 * @param request  - 컨트롤러의 워크스페이스 생성 요청 객체
	 * @param memberId - 컨트롤러에서의 로그인 정보에서 꺼내온 멤버 id(PK)
	 * @return WorkspaceCreateResponse - 워크스페이스 생성 응답 DTO
	 */
	@Override
	@Transactional
	public CreateWorkspaceResponse createWorkspace(
		CreateWorkspaceRequest request,
		Long memberId
	) {
		Member member = memberQueryService.findMember(memberId);

		Workspace workspace = CreateWorkspaceRequest.to(request);
		setUniqueWorkspaceCode(workspace);
		setEncodedPasswordIfPresent(request, workspace);
		setKeyPrefix(request, workspace);

		Workspace savedWorkspace = workspaceRepository.save(workspace);

		WorkspaceMember workspaceMember = WorkspaceMember.addOwnerWorkspaceMember(
			member,
			savedWorkspace,
			randomNicknameGenerator.generateNickname()
		);
		workspaceMemberRepository.save(workspaceMember);

		return CreateWorkspaceResponse.from(savedWorkspace);
	}

	private Optional<String> generateUniqueWorkspaceCode() {
		for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
			String code = workspaceCodeGenerator.generateWorkspaceCode();

			if (isWorkspaceCodeUnique(code)) {
				return Optional.of(code);
			}
			log.info("Workspace code collision occured. Retry attempt: #{}", attempt);
		}
		return Optional.empty();
	}

	private void setKeyPrefix(CreateWorkspaceRequest request, Workspace workspace) {
		workspace.updateIssueKeyPrefix(request.issueKeyPrefix());
	}

	private void setUniqueWorkspaceCode(Workspace workspace) {
		String code = generateUniqueWorkspaceCode()
			.orElseThrow(() -> new InternalServerException(
				String.format("Failed to solve workspace code collision. Max retry limit: %d", MAX_RETRIES)));

		workspace.setCode(code);
	}

	private void setEncodedPasswordIfPresent(CreateWorkspaceRequest request, Workspace workspace) {
		String encodedPassword = Optional.ofNullable(request.password())
			.map(passwordEncoder::encode)
			.orElse(null);
		workspace.updatePassword(encodedPassword);
	}

	public boolean isWorkspaceCodeUnique(String code) {
		return !workspaceRepository.existsByCode(code);
	}
}
