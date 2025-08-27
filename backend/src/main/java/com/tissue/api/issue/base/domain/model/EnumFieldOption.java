package com.tissue.api.issue.base.domain.model;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.util.TextNormalizer;
import com.tissue.api.global.key.KeyGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = {
	@UniqueConstraint(columnNames = {"field_id", "label"})
})
public class EnumFieldOption extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "field_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private IssueField field;

	@Column(nullable = false, updatable = false, unique = true)
	private String key;

	@Column(nullable = false)
	private String label;

	@Column(nullable = false)
	private int position; // 정렬

	// private String color;

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
		this.label = TextNormalizer.stripToEmpty(label);
		this.position = (position == null) ? 0 : position;
		// this.archived = false;
	}

	public void rename(String label) {
		this.label = label;
	}

	public void movePositionTo(int position) {
		this.position = position;
	}
}
