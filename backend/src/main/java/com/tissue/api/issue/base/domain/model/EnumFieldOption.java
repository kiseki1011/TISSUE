package com.tissue.api.issue.base.domain.model;

import org.hibernate.annotations.SQLRestriction;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.util.TextNormalizer;
import com.tissue.api.global.key.KeyGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostPersist;
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
public class EnumFieldOption extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "issue_field_id", nullable = false)
	private IssueField field;

	@Column(nullable = false, updatable = false, unique = true)
	private String key;

	@Column(nullable = false)
	private String label;

	@Column(nullable = false)
	private int position;

	// private ColorType color;

	@PostPersist
	private void assignKey() {
		if (key == null && id != null) {
			key = KeyGenerator.generateEnumFieldOptionKey(id);
		}
	}

	@Builder
	public EnumFieldOption(
		IssueField field,
		String label,
		Integer position
	) {
		this.field = field;
		this.label = TextNormalizer.normalizeText(label);
		this.position = (position == null) ? 0 : position;
	}

	public void rename(String label) {
		// TODO: TextPreconditions.requireNotNull(label);
		this.label = TextNormalizer.normalizeText(label);
	}

	public void movePositionTo(int position) {
		this.position = position;
	}

	public void softDelete() {
		archive();
	}
}
