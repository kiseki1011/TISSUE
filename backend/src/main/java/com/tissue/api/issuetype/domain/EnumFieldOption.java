package com.tissue.api.issuetype.domain;

import org.hibernate.annotations.SQLRestriction;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.issue.domain.model.vo.Label;

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
import lombok.NonNull;
import lombok.ToString;

@Entity
@SQLRestriction("archived = false")
@Table(
	indexes = {
		@Index(name = "idx_option_field_label", columnList = "issue_field_id,label"),
		@Index(name = "idx_option_field_position", columnList = "issue_field_id,position")
	}
)
@Getter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnumFieldOption extends BaseEntity {

	// @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "field_option_seq_gen")
	// @SequenceGenerator(name = "field_option_seq_gen", sequenceName = "field_option_seq", allocationSize = 50)
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@ToString.Include
	private Long id;

	@Version
	@ToString.Include
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "issue_field_id", nullable = false)
	private IssueField field;

	@Embedded
	@ToString.Include
	private Label label;

	@Column(nullable = false)
	private int position;

	@Builder(access = AccessLevel.PRIVATE)
	private EnumFieldOption(
		IssueField field,
		Label label,
		Integer position
	) {
		this.field = field;
		this.label = label;
		this.position = position;
	}

	public static EnumFieldOption create(
		@NonNull IssueField field,
		@NonNull Label label,
		Integer position
	) {
		return EnumFieldOption.builder()
			.field(field)
			.label(label)
			.position((position == null) ? 0 : position)
			.build();
	}

	public void rename(@NonNull Label label) {
		this.label = label;
	}

	public void movePositionTo(int position) {
		this.position = position;
	}

	public void softDelete() {
		archive();
	}
}
