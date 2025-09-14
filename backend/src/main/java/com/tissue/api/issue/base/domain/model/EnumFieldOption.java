package com.tissue.api.issue.base.domain.model;

import org.hibernate.annotations.SQLRestriction;

import com.tissue.api.common.entity.PrefixedKeyEntity;
import com.tissue.api.common.util.DomainPreconditions;
import com.tissue.api.global.key.KeyPrefixPolicy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@SQLRestriction("archived = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	// uniqueConstraints = @UniqueConstraint(columnNames = {"issue_field_id", "label"}),
	indexes = {
		@Index(name = "idx_option_field_label", columnList = "issue_field_id,label"),
		@Index(name = "idx_option_field_position", columnList = "issue_field_id,position")
	}
)
public class EnumFieldOption extends PrefixedKeyEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "field_option_seq_gen")
	@SequenceGenerator(name = "field_option_seq_gen", sequenceName = "field_option_seq", allocationSize = 50)
	private Long id;

	@Version
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "issue_field_id", nullable = false)
	private IssueField field;

	@Column(name = "field_option_key", nullable = false, updatable = false, unique = true)
	private String key;

	@Column(nullable = false)
	private String label;

	@Column(nullable = false)
	private int position;

	@Override
	protected void setKey(String key) {
		this.key = key;
	}

	@Override
	protected String keyPrefix() {
		return KeyPrefixPolicy.ISSUE_ENUM_FIELD_OPTION;
	}

	@Builder
	public EnumFieldOption(
		IssueField field,
		String label,
		Integer position
	) {
		this.field = DomainPreconditions.requireNotNull(field, "issueField");
		this.label = DomainPreconditions.requireNotBlank(label, "label");
		this.position = (position == null) ? 0 : position;
	}

	public void rename(String label) {
		this.label = DomainPreconditions.requireNotBlank(label, "label");
	}

	public void movePositionTo(int position) {
		this.position = position;
	}

	public void softDelete() {
		archive();
	}
}
