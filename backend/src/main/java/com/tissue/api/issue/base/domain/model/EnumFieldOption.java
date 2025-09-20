package com.tissue.api.issue.base.domain.model;

import java.util.Objects;

import org.hibernate.annotations.SQLRestriction;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.util.DomainPreconditions;
import com.tissue.api.issue.base.domain.model.vo.Label;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@ToString(onlyExplicitlyIncluded = true)
@SQLRestriction("archived = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	// uniqueConstraints = @UniqueConstraint(columnNames = {"issue_field_id", "label"}),
	indexes = {
		@Index(name = "idx_option_field_label", columnList = "issue_field_id,label"),
		@Index(name = "idx_option_field_position", columnList = "issue_field_id,position")
	}
)
public class EnumFieldOption extends BaseEntity {

	// @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "field_option_seq_gen")
	// @SequenceGenerator(name = "field_option_seq_gen", sequenceName = "field_option_seq", allocationSize = 50)
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@ToString.Include
	private Long id;

	@Version
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "issue_field_id", nullable = false)
	private IssueField field;

	@Embedded
	@ToString.Include
	private Label label;

	@Column(nullable = false)
	private int position;

	@Builder
	private EnumFieldOption(
		IssueField field,
		Label label,
		Integer position
	) {
		this.field = DomainPreconditions.requireNotNull(field, "issueField");
		this.label = Objects.requireNonNull(label);
		this.position = (position == null) ? 0 : position;
	}

	public static EnumFieldOption create(IssueField field, Label label, Integer position) {
		return EnumFieldOption.builder()
			.field(field)
			.label(label)
			.position(position)
			.build();
	}

	public void rename(Label label) {
		this.label = Objects.requireNonNull(label);
	}

	public void movePositionTo(int position) {
		this.position = position;
	}

	public void softDelete() {
		archive();
	}
}
