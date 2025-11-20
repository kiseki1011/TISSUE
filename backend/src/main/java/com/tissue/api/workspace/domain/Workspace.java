package com.tissue.api.workspace.domain;

import static com.tissue.api.common.util.DomainPreconditions.*;
import static com.tissue.api.workspace.domain.enums.WorkspaceRole.*;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;
import com.tissue.api.workspace.domain.exception.WorkspaceOwnershipRequiredException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@SQLRestriction("softDeleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workspace extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "workspace_id")
	private Long id;

	@Column(name = "workspace_key", unique = true, nullable = false)
	private String key;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String description;

	@OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkspaceMember> workspaceMembers = new ArrayList<>();

	public static Workspace create(
		@NonNull String key,
		@NonNull String name,
		@Nullable String description,
		@NonNull Member member
	) {
		Workspace workspace = new Workspace();
		workspace.key = key;
		workspace.name = name;
		workspace.description = nullToEmpty(description);

		workspace.workspaceMembers.add(WorkspaceMember.create(member, workspace, OWNER));

		return workspace;
	}

	public WorkspaceMember addMember(@NonNull Member member, @NonNull WorkspaceRole role) {
		WorkspaceMember workspaceMember = WorkspaceMember.create(
			member,
			this,
			role
		);
		this.workspaceMembers.add(workspaceMember);

		return workspaceMember;
	}

	public void removeMember(@NonNull WorkspaceMember workspaceMember) {
		this.workspaceMembers.remove(workspaceMember);
	}

	public void transferOwnership(@NonNull WorkspaceMember owner, @NonNull WorkspaceMember newOwner) {
		if (!owner.isOwner()) {
			throw new WorkspaceOwnershipRequiredException("Needs to be OWNER to transfer ownership.",
				key, owner.getMemberId(), owner.getRole());
		}
		owner.changeRoleTo(ADMIN);
		newOwner.changeRoleToOwner();
	}

	public void updateName(@NonNull String name) {
		this.name = name;
	}

	public void updateDescription(@Nullable String description) {
		this.description = nullToEmpty(description);
	}

	public int getMemberCount() {
		return workspaceMembers.size();
	}
}
