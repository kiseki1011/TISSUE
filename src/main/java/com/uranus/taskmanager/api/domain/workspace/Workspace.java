package com.uranus.taskmanager.api.domain.workspace;

import java.util.ArrayList;
import java.util.List;

import com.uranus.taskmanager.api.domain.workspaceuser.WorkspaceMember;

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
public class Workspace {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "WORKSPACE_ID")
	private Long id;

	@Column(unique = true) //  nullable = false 사용 고려
	private String workspaceCode;

	@Column(nullable = false)
	private String name;

	private String description;

	@OneToMany(mappedBy = "workspace")
	private List<WorkspaceMember> workspaceMembers = new ArrayList<>();
	
	@Builder
	public Workspace(String workspaceCode, String name, String description) {
		this.workspaceCode = workspaceCode;
		this.name = name;
		this.description = description;
	}

	public void setWorkspaceCode(String workspaceCode) {
		this.workspaceCode = workspaceCode;
	}

}
