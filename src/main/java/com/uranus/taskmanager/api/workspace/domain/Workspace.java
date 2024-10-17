package com.uranus.taskmanager.api.workspace.domain;

import java.util.ArrayList;
import java.util.List;

import com.uranus.taskmanager.api.common.entity.BaseEntity;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workspace extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "WORKSPACE_ID")
	private Long id;

	@Column(unique = true) //  nullable = false 사용 고려
	private String code;

	@Column(nullable = false)
	private String name;

	private String password;
	private String description;

	@OneToMany(mappedBy = "workspace")
	private List<WorkspaceMember> workspaceMembers = new ArrayList<>();

	@OneToMany(mappedBy = "workspace")
	private List<Invitation> invitations = new ArrayList<>();

	@Builder
	public Workspace(String code, String name, String description, String password) {
		this.code = code;
		this.name = name;
		this.description = description;
		this.password = password;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
