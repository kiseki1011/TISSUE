package com.tissue.fixture.repository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.Member;
import com.tissue.api.security.PasswordEncoder;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;

@Component
@Transactional
public class WorkspaceRepositoryFixture {
	@Autowired
	private WorkspaceRepository workspaceRepository;
	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;

	/**
	 * 워크스페이스를 생성하고 저장합니다.
	 *
	 * @param name        - 워크스페이스 이름
	 * @param description - 워크스페이스 설명
	 * @param code        - 워크스페이스의 8자리 코드 (Base62, 중복 비허용)
	 * @param password    - 워크스페이스의 비밀번호 (null 값 허용, 비밀번호가 없는 경우 null 전달, 있으면 암호화)
	 * @return 저장된 Workspace 객체
	 */
	public Workspace createAndSaveWorkspace(String name, String description, String code, String password) {
		Workspace workspace = Workspace.builder()
			.name(name)
			.description(description)
			.code(code)
			.password(encodePasswordIfPresent(password))
			.build();
		return workspaceRepository.save(workspace);
	}

	/**
	 * 워크스페이스 멤버를 생성하고 저장합니다.
	 *
	 * @param member    - Member 객체
	 * @param workspace - Workspace 객체
	 * @param role      - 워크스페이스 내 역할
	 * @return 저장된 WorkspaceMember 객체
	 */
	public WorkspaceMember addAndSaveMemberToWorkspace(Member member, Workspace workspace, WorkspaceRole role) {
		WorkspaceMember workspaceMember = WorkspaceMember.addWorkspaceMember(
			member, workspace, role, member.getEmail()
		);
		return workspaceMemberRepository.save(workspaceMember);
	}

	private String encodePasswordIfPresent(String password) {
		return Optional.ofNullable(password)
			.filter(pw -> !pw.isEmpty())
			.map(passwordEncoder::encode)
			.orElse(null);
	}
}
