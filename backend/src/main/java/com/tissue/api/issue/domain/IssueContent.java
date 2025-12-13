package com.tissue.api.issue.domain;

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

	// TODO: 제거 고려할까? 추후에 AI 요약 같은 기능을 도입할 가능성 때문에 이 필드를 추가해놓긴 했는데,
	//  굳이 필요한가 고민이 됨.
	@Lob
	@Column(name = "summary")
	private String summary;

	public static IssueContent of(@Nullable String content, @Nullable String summary) {
		IssueContent issueContent = new IssueContent();
		issueContent.content = nullToEmpty(content);
		issueContent.summary = nullToEmpty(summary);

		return issueContent;
	}

	void updateContent(@Nullable String content) {
		this.content = nullToEmpty(content);
	}

	void updateSummary(@Nullable String summary) {
		this.summary = nullToEmpty(summary);
	}
}
