package com.uranus.taskmanager.api.workspace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.authentication.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.security.PasswordEncoder;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceResponse;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.util.WorkspaceCodeGenerator;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * HandleDatabaseExceptionService와 다르게 WorkspaceCode의 중복 검사를 진행한다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckCodeDuplicationService implements WorkspaceCreateService {

	private static final int MAX_RETRIES = 5;

	private final WorkspaceRepository workspaceRepository;
	private final MemberRepository memberRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceCodeGenerator workspaceCodeGenerator;
	private final PasswordEncoder passwordEncoder;

	/**
	 * Todo: createWorkspace() 가독성 좋은 코드로 리팩토링
	 * <p>
	 * 코드 과정
	 * <p>
	 * 1. ArgumentResolver를 통해 세션의 Member 정보를 컨트롤러로 넘긴다(LoginMemberDto라는 형태로)
	 * 	- Todo: 과연 Member 엔티티를 반환하지 않고 DTO 형태로 컨트롤러로 넘기는게 과연 잘한걸까?
	 * 	- Todo: OSIV 관련 문제 찾아보기
	 * 2. 컨트롤러에서 넘어온 생성 request, 세션에 저장된 로그인 정보 loginMember을 사용한다
	 * 3. loginMember의 loginId를 사용해 Member 엔티티를 찾는다
	 * 	- Todo: 컨트롤러에서 loginId만 받아와도 되는거 아닌가?
	 * 4. 기존 워크스페이스 코드 생성과 워크스페이스 저장 과정
	 * 5. WorkspaceMember도 같이 생성한다(addWorkspaceMember라는 편의 메서드를 통해서)
	 * 	- 워크스페이스를 생성한 멤버는 해당 워크스페이스에서 기본적으로 ADMIN 권한을 가진다
	 * 	- 별칭(nickname)은 기본적으로 email로 설정된다
	 * 6. WorkspaceMember를 저장한다
	 */
	@Override
	@Transactional
	public WorkspaceResponse createWorkspace(WorkspaceCreateRequest request, LoginMemberDto loginMember) {

		/*
		 * Todo: loginMember 검증 로직이 필요할까?
		 */
		Member member = memberRepository.findByLoginId(loginMember.getLoginId())
			.orElseThrow(MemberNotFoundException::new);

		for (int count = 0; count < MAX_RETRIES; count++) {
			String code = workspaceCodeGenerator.generateWorkspaceCode();
			if (workspaceCodeIsNotDuplicate(code)) {
				log.info("[workspaceCodeIsNotDuplicate] code = {}", code);

				// 요청 DTO 객체에 생성된 code 설정
				request.setCode(code);

				// 요청 DTO 객체에 비밀번호를 꺼내고 암호화하고 설정
				// Todo: null 대신 Optional을 사용, Optional은 바로 해소하도록 로직 작성
				if (request.getPassword() != null) {
					String encodedPassword = passwordEncoder.encode(request.getPassword());
					request.setPassword(encodedPassword);
				}

				// 요청 DTO를 사용해서 워크스페이스 엔티티로 만들고 저장
				Workspace workspace = workspaceRepository.save(request.to());

				// 워크스페이스 멤버 생성 및 저장
				WorkspaceMember workspaceMember = WorkspaceMember.addWorkspaceMember(member, workspace,
					WorkspaceRole.ADMIN,
					member.getEmail());

				workspaceMemberRepository.save(workspaceMember);

				return WorkspaceResponse.from(workspace);
			}
			log.info("[Workspace Code Collision] Retrying... attempt {}", count + 1);
		}
		throw new RuntimeException(
			"Failed to solve workspace code collision"); // Todo: WorkspaceCodeCollisionHandleException 구현
	}

	public boolean workspaceCodeIsNotDuplicate(String code) {
		return !workspaceRepository.existsByCode(code);
	}
}
