package com.tissue.issuetype.domain;

import org.springframework.lang.Nullable;

import com.tissue.common.entity.BaseEntity;
import com.tissue.common.vo.Label;
import com.tissue.issuetype.domain.enums.IssueFieldType;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueField extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	private Long version;

	@Embedded
	private Label label;

	@Column(nullable = false, length = 255)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IssueFieldType issueFieldType;

	@Column(nullable = false)
	private boolean required;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "issue_type_id", nullable = false)
	private IssueType issueType;

	// private String icon;

	public static IssueField create(
		@NonNull Label label,
		@Nullable String description,
		@NonNull IssueFieldType issueFieldType,
		boolean required,
		@NonNull IssueType issueType
	) {
		IssueField issueField = new IssueField();

		issueField.label = label;
		issueField.description = description;
		issueField.issueFieldType = issueFieldType;
		issueField.required = required;
		issueField.issueType = issueType;

		return issueField;
	}

	public String getWorkspaceKey() {
		return issueType.getWorkspaceKey();
	}

	public String getDisplayLabel() {
		return label.getDisplay();
	}

	public void rename(@NonNull Label label) {
		this.label = label;
	}

	public void updateDescription(@Nullable String description) {
		this.description = description;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}
}
