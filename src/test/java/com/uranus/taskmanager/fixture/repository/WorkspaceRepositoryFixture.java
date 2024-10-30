package com.uranus.taskmanager.fixture.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;

@Component
@Transactional
public class WorkspaceRepositoryFixture {
	@Autowired
	private WorkspaceRepository workspaceRepository;
	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;

	/**
	 * 워크스페이스를 생성하고 저장합니다.
	 *
	 * @param name - 워크스페이스 이름
	 * @param description - 워크스페이스 설명
	 * @param code - 워크스페이스의 8자리 코드 (Base62, 중복 비허용)
	 * @param password - 워크스페이스의 비밀번호 (null 값 허용, 비밀번호가 없는 경우 null 전달)
	 * @return 저장된 Workspace 객체
	 */
	public Workspace createWorkspace(String name, String description, String code, String password) {
		Workspace workspace = Workspace.builder()
			.name(name)
			.description(description)
			.code(code)
			.password(password) // 요청에 비밀번호를 명시하지 않으면 null
			.build();
		return workspaceRepository.save(workspace);
	}

	/**
	 * 워크스페이스 멤버를 생성하고 저장합니다.
	 *
	 * @param member - Member 객체
	 * @param workspace - Workspace 객체
	 * @param role - 워크스페이스 내 역할
	 * @return 저장된 WorkspaceMember 객체
	 */
	public WorkspaceMember addMemberToWorkspace(Member member, Workspace workspace, WorkspaceRole role) {
		WorkspaceMember workspaceMember = WorkspaceMember.addWorkspaceMember(
			member, workspace, role, member.getEmail()
		);
		return workspaceMemberRepository.save(workspaceMember);
	}
}
