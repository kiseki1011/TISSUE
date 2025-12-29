package com.tissue.security.authentication;

import com.tissue.member.application.port.out.MemberQueryRepository;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.MemberStatus;
import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.project.domain.ProjectMember;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.domain.WorkspaceMember;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring security uses this service to search UserDetails
 *
 * <p>Gets Member from the DB using the email extracted from the JWT. Then converts the Member to
 * MemberUserDetails.
 *
 * <p>Why is this needed?
 * <li>Spring security cannot know the user's current information only with the JWT
 * <li>A user's role or status can be changed anytime
 */
@Service
@RequiredArgsConstructor
public class MemberUserDetailsService implements UserDetailsService {

    private final MemberQueryRepository memberRepository;
    private final WorkspaceMemberQueryRepository workspaceMemberRepository;
    private final ProjectMemberQueryRepository projectMemberRepository;

    /** Find by username(in this case email) extracted from the JWT token */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository
                .findByEmailAndStatus(email, MemberStatus.ACTIVE)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found for email: " + email));

        var workspaceRoles = workspaceMemberRepository.findAllByMember(member).stream()
                .collect(Collectors.toMap(WorkspaceMember::getWorkspaceKey, WorkspaceMember::getRole));

        var projectRoles = projectMemberRepository.findAllByMemberId(member.getId()).stream()
                .collect(Collectors.groupingBy(
                        ProjectMember::getWorkspaceKey,
                        Collectors.toMap(ProjectMember::getProjectKey, ProjectMember::getRole)));

        return new MemberUserDetails(member, workspaceRoles, projectRoles);
    }
}
