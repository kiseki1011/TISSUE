package com.tissue.api.issue.domain.newmodel;

import com.tissue.api.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
// @EqualsAndHashCode(of = {"name", "issueType"}, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomFieldDefinition extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	private String description;

	private String fieldKey; // used as key in JSON storage

	@Enumerated(EnumType.STRING)
	private IssueFieldType fieldType; // TEXT, NUMBER, DATE, ENUM, etc.

	private boolean required;

	@ManyToOne(fetch = FetchType.LAZY)
	private IssueTypeDefinition issueType;
}
