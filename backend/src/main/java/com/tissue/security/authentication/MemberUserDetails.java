package com.tissue.security.authentication;

import com.tissue.member.domain.Member;
import com.tissue.member.domain.MemberStatus;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.security.authorization.SystemRole;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class MemberUserDetails implements UserDetails {

    private final Long memberId;
    private final String email;
    private final String username;
    private final String password;
    private final SystemRole role;
    private final MemberStatus status;

    private final Collection<? extends GrantedAuthority> authorities;

    private boolean elevated;

    // TODO: should i make a separate class instead of using Map?
    // TODO: does caching these roles cause bad performance? or is it tolerable?
    private final Map<String, WorkspaceRole> workspaceRoles;
    private final Map<String, Map<String, ProjectRole>> projectRoles;

    public MemberUserDetails(
            Member member,
            Map<String, WorkspaceRole> workspaceRoles,
            Map<String, Map<String, ProjectRole>> projectRoles) {
        this.memberId = member.getId();
        this.email = member.getEmail();
        this.username = member.getUsername();
        this.password = member.getPassword();
        this.role = member.getRole();
        this.status = member.getStatus();

        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(role.getAuthority()));

        this.workspaceRoles = workspaceRoles != null ? workspaceRoles : Collections.emptyMap();
        this.projectRoles = projectRoles != null ? projectRoles : Collections.emptyMap();
    }

    public MemberUserDetails(Member member) {
        this(member, Collections.emptyMap(), Collections.emptyMap());
    }

    public void setElevated(boolean elevated) {
        this.elevated = elevated;
    }

    public boolean hasWorkspaceRole(String workspaceKey, WorkspaceRole role) {
        WorkspaceRole myRole = workspaceRoles.get(workspaceKey);
        return myRole != null && myRole.isEqualOrHigherThan(role);
    }

    public boolean hasProjectRole(String workspaceKey, String projectKey, ProjectRole role) {
        if (hasWorkspaceRole(workspaceKey, WorkspaceRole.ADMIN)) {
            return true;
        }

        Map<String, ProjectRole> projectRoleMap = projectRoles.get(workspaceKey);
        if (projectRoleMap == null) {
            return false;
        }
        ProjectRole myRole = projectRoleMap.get(projectKey);
        return myRole != null && myRole.isEqualOrHigherThan(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status != MemberStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.status != MemberStatus.DELETED;
    }
}
