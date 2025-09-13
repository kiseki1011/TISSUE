package deprecated.com.tissue.unit.service.command;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tissue.api.notification.application.service.command.NotificationTargetService;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;

@ExtendWith(MockitoExtension.class)
public class NotificationTargetServiceTest {

	@Mock
	private WorkspaceMemberRepository workspaceMemberRepository;

	@InjectMocks
	private NotificationTargetService notificationTargetService;

	// @Test
	// void should_return_admins_and_specific_member_when_both_exist() {
	// given
	// 	String workspaceCode = "WS-001";
	// 	Long specificMemberId = 100L;
	//
	// 	Workspace workspace = Workspace.builder()
	// 		.code(workspaceCode)
	// 		.name("Dev")
	// 		.description("Development Workspace")
	// 		.password("pass123")
	// 		.issueKeyPrefix("DEV")
	// 		.build();
	//
	// 	Member admin1 = Member.builder()
	// 		.loginId("admin1")
	// 		.email("admin1@example.com")
	// 		.username("admin1")
	// 		.password("pass")
	// 		.name("Admin One")
	// 		.jobType(JobType.DEVELOPER)
	// 		.birthDate(LocalDate.of(1990, 1, 1))
	// 		.build();
	//
	// 	Member specificMember = Member.builder()
	// 		.loginId("user1")
	// 		.email("user1@example.com")
	// 		.username("user1")
	// 		.password("pass")
	// 		.name("User One")
	// 		.jobType(JobType.DESIGNER)
	// 		.birthDate(LocalDate.of(1995, 5, 5))
	// 		.build();
	//
	// 	WorkspaceMember adminMember = WorkspaceMember.createWorkspaceMember(admin1, workspace, WorkspaceRole.ADMIN);
	// 	WorkspaceMember specificWorkspaceMember = WorkspaceMember.createWorkspaceMember(specificMember, workspace,
	// 		WorkspaceRole.MEMBER);
	//
	// 	when(workspaceMemberRepository.findAdminsByWorkspaceKey(workspaceCode))
	// 		.thenReturn(new HashSet<>(List.of(adminMember)));
	//
	// 	when(workspaceMemberRepository.findByMemberIdAndWorkspaceKey(specificMemberId, workspaceCode))
	// 		.thenReturn(Optional.of(specificWorkspaceMember));
	//
	// 	// when
	// 	Set<WorkspaceMember> result = notificationTargetService.getAdminAndSpecificMemberTargets(workspaceCode,
	// 		specificMemberId);
	//
	// 	// then
	// 	assertThat(result)
	// 		.containsExactlyInAnyOrder(adminMember, specificWorkspaceMember)
	// 		.hasSize(2);
	// }
	//
	// @Test
	// void should_return_only_admins_when_specific_member_not_found() {
	// 	// given
	// 	String workspaceCode = "WS-002";
	// 	Long nonExistentMemberId = 999L;
	//
	// 	Workspace workspace = Workspace.builder()
	// 		.code(workspaceCode)
	// 		.name("QA")
	// 		.description("QA Workspace")
	// 		.password("secure")
	// 		.issueKeyPrefix("QA")
	// 		.build();
	//
	// 	Member admin = Member.builder()
	// 		.loginId("admin")
	// 		.email("admin@example.com")
	// 		.username("admin")
	// 		.password("pass")
	// 		.name("Qa Admin")
	// 		.jobType(JobType.DEVELOPER)
	// 		.birthDate(LocalDate.of(1985, 2, 2))
	// 		.build();
	//
	// 	WorkspaceMember adminMember = WorkspaceMember.createWorkspaceMember(admin, workspace, WorkspaceRole.ADMIN);
	//
	// 	when(workspaceMemberRepository.findAdminsByWorkspaceKey(workspaceCode))
	// 		.thenReturn(Set.of(adminMember));
	//
	// 	when(workspaceMemberRepository.findByMemberIdAndWorkspaceKey(nonExistentMemberId, workspaceCode))
	// 		.thenReturn(Optional.empty());
	//
	// 	// when
	// 	Set<WorkspaceMember> result = notificationTargetService.getAdminAndSpecificMemberTargets(workspaceCode,
	// 		nonExistentMemberId);
	//
	// 	// then
	// 	assertThat(result)
	// 		.containsExactly(adminMember)
	// 		.hasSize(1);
	// }
}
