package com.tissue.unit.service.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.domain.model.enums.JobType;
import com.tissue.api.member.domain.model.vo.Name;
import com.tissue.api.notification.application.service.command.NotificationTargetService;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;

@ExtendWith(MockitoExtension.class)
public class NotificationTargetServiceTest {

	@Mock
	private WorkspaceMemberRepository workspaceMemberRepository;

	@InjectMocks
	private NotificationTargetService notificationTargetService;

	@Test
	void should_return_admins_and_specific_member_when_both_exist() {
		// given
		String workspaceCode = "WS-001";
		Long specificMemberId = 100L;

		Workspace workspace = Workspace.builder()
			.code(workspaceCode)
			.name("Dev")
			.description("Development Workspace")
			.password("pass123")
			.issueKeyPrefix("DEV")
			.build();

		Member admin1 = Member.builder()
			.loginId("admin1")
			.email("admin1@example.com")
			.username("admin1")
			.password("pass")
			.name(new Name("Admin", "One"))
			.jobType(JobType.DEVELOPER)
			.birthDate(LocalDate.of(1990, 1, 1))
			.build();

		Member specificMember = Member.builder()
			.loginId("user1")
			.email("user1@example.com")
			.username("user1")
			.password("pass")
			.name(new Name("User", "One"))
			.jobType(JobType.DESIGNER)
			.birthDate(LocalDate.of(1995, 5, 5))
			.build();

		WorkspaceMember adminMember = WorkspaceMember.createWorkspaceMember(admin1, workspace, WorkspaceRole.ADMIN);
		WorkspaceMember specificWorkspaceMember = WorkspaceMember.createWorkspaceMember(specificMember, workspace,
			WorkspaceRole.MEMBER);

		when(workspaceMemberRepository.findAdminsByWorkspaceCode(workspaceCode))
			.thenReturn(new HashSet<>(List.of(adminMember)));

		when(workspaceMemberRepository.findByMemberIdAndWorkspaceCode(specificMemberId, workspaceCode))
			.thenReturn(Optional.of(specificWorkspaceMember));

		// when
		Set<WorkspaceMember> result = notificationTargetService.getAdminAndSpecificMemberTargets(workspaceCode,
			specificMemberId);

		// then
		assertThat(result)
			.containsExactlyInAnyOrder(adminMember, specificWorkspaceMember)
			.hasSize(2);
	}

	@Test
	void should_return_only_admins_when_specific_member_not_found() {
		// given
		String workspaceCode = "WS-002";
		Long nonExistentMemberId = 999L;

		Workspace workspace = Workspace.builder()
			.code(workspaceCode)
			.name("QA")
			.description("QA Workspace")
			.password("secure")
			.issueKeyPrefix("QA")
			.build();

		Member admin = Member.builder()
			.loginId("admin")
			.email("admin@example.com")
			.username("admin")
			.password("pass")
			.name(new Name("QA", "Admin"))
			.jobType(JobType.DEVELOPER)
			.birthDate(LocalDate.of(1985, 2, 2))
			.build();

		WorkspaceMember adminMember = WorkspaceMember.createWorkspaceMember(admin, workspace, WorkspaceRole.ADMIN);

		when(workspaceMemberRepository.findAdminsByWorkspaceCode(workspaceCode))
			.thenReturn(Set.of(adminMember));

		when(workspaceMemberRepository.findByMemberIdAndWorkspaceCode(nonExistentMemberId, workspaceCode))
			.thenReturn(Optional.empty());

		// when
		Set<WorkspaceMember> result = notificationTargetService.getAdminAndSpecificMemberTargets(workspaceCode,
			nonExistentMemberId);

		// then
		assertThat(result)
			.containsExactly(adminMember)
			.hasSize(1);
	}
}
