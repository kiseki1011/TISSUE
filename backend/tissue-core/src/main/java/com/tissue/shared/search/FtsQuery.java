package com.tissue.shared.search;

import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Builds PostgreSQL {@code to_tsquery} input from text user keywords, with prefix matching.
 *
 * <p>Each whitespace-separated term becomes a prefix term ({@code term:*}) and the terms are ANDed,
 * <pre>
 * example: {@code "depl gui" -> "depl:* & gui:*"}
 * </pre>
 * So a user typing a partial word ("depl") matches documents containing a word that starts with it
 * ("deployment"). Characters that aren't letters or digits are stripped, so the result is always valid
 * {@code to_tsquery} syntax (raw input would trip on operators like {@code & | ! : ( )}). Pairs with
 * the {@code to_tsquery('simple', ?)} Hibernate functions.
 *
 * <p>Returns {@code ""} when no usable term remains - {@code to_tsquery('simple', '')} is an empty query
 * that matches nothing.
 */
@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        model = "claude-opus-4-8",
        evaluation = Evaluation.NOT_REVIEWED)
public final class FtsQuery {

    private static final Pattern NON_TERM_CHARS = Pattern.compile("[^\\p{L}\\p{N}\\s]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private FtsQuery() {}

    public static String toPrefixQuery(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String cleaned = NON_TERM_CHARS.matcher(raw).replaceAll(" ").trim();
        if (cleaned.isEmpty()) {
            return "";
        }
        StringBuilder query = new StringBuilder();
        for (String term : WHITESPACE.split(cleaned)) {
            boolean queryNotEmpty = !query.isEmpty();
            if (queryNotEmpty) {
                query.append(" & ");
            }
            query.append(term).append(":*");
        }
        return query.toString();
    }
}
