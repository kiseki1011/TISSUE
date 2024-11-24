package com.uranus.taskmanager.api.invitation.validator;

import org.springframework.stereotype.Component;

import com.uranus.taskmanager.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.api.workspacemember.exception.AlreadyJoinedWorkspaceException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InvitationValidator {

	private final WorkspaceMemberRepository workspaceMemberRepository;

	/**
	 * Todo
	 *  - 중복이 되더라도 WorkspaceMemberValidator도 비슷한 메서드를 재정의
	 *  - Invitation과 WorkspaceMember 영역이 강하게 결합될 수 밖에 없는 구조인듯
	 *  - 퍼사드 패턴(Facade Pattern)의 적용을 고려했지만 굳이 안해도 될 것 같음
	 *  - -> 일단 서비스에서 다른 서비스를 호출 하지 않을 것이기 때문에, 순환 참조의 발생은 없음
	 */
	public void validateInvitation(Long memberId, String workspaceCode) {
		if (isAlreadyWorkspaceMember(memberId, workspaceCode)) {
			throw new AlreadyJoinedWorkspaceException();
		}
	}

	private boolean isAlreadyWorkspaceMember(Long memberId, String workspaceCode) {
		return workspaceMemberRepository.existsByMemberIdAndWorkspaceCode(memberId, workspaceCode);
	}

	/*
	 * Todo
	 *  - 멤버가 참여할 수 있는 여유 공간이 있는지 확인하는 검증 추가 예정
	 *    - 워크스페이스가 최대 멤버수에 도달했는지 검증하기
	 *  - WorkspaceMemberLimitExceededException(500명 제한) 만들기
	 */

}
