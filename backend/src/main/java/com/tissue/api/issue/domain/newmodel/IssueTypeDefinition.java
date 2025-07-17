package com.tissue.api.issue.domain.newmodel;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.workspace.domain.model.Workspace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
// @EqualsAndHashCode(of = {"name", "workspace"}, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueTypeDefinition extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name; // e.g., "Epic", "Bug", "CustomType1"

	private String icon;

	@Column(nullable = false)
	private String color;

	private boolean systemType; // true = built-in default types

	@ManyToOne(fetch = FetchType.LAZY)
	private Workspace workspace;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowDefinition workflow;

	private boolean isCustom; // true = uses dynamic fields
}
