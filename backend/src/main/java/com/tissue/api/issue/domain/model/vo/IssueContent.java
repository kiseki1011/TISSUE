package com.tissue.api.issue.domain.model.vo;

import static com.tissue.api.common.util.DomainPreconditions.*;

import org.springframework.lang.Nullable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueContent {

	@Lob
	@Column(name = "content")
	private String content;

	@Lob
	@Column(name = "summary")
	private String summary;

	public static IssueContent of(@Nullable String content, @Nullable String summary) {
		IssueContent issueContent = new IssueContent();
		issueContent.content = nullToEmpty(content);
		issueContent.summary = nullToEmpty(summary);

		return issueContent;
	}

	public void updateContent(@Nullable String content) {
		this.content = nullToEmpty(content);
	}

	public void updateSummary(@Nullable String summary) {
		this.summary = nullToEmpty(summary);
	}
}
