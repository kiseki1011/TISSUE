package com.tissue.mcp.tool;

import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.ACTOR_DISPLAY_NAME;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.ASSIGNEE_DISPLAY_NAME;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.BRANCH_NAME;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.NEW_PARENT_KEY;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.NEW_POINT;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.NEW_STATE;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.OLD_PARENT_KEY;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.OLD_POINT;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.OLD_STATE;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.PR_ACTION;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.PR_TITLE;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.RELATION_TYPE;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.REMOVED_ASSIGNEE_DISPLAY_NAME;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.REMOVED_REVIEWER_DISPLAY_NAME;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.REVIEWER_COUNT;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.REVIEWER_DISPLAY_NAME;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.REVIEW_STATUS;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.SPRINT_TITLE;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.TARGET_ISSUE_KEY;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.TRIGGER_REASON;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.VCS_USER_NAME;

import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.shared.dto.FieldChange;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/**
 * Turns a stored activity log row into one English sentence.
 *
 * <p>The stored row is a type plus a bag of string keys, which an agent would otherwise have to learn to
 * decode; a sentence carries the same fact in a form it already reads. The wording is built here rather
 * than in the domain because it is a presentation choice for one consumer - the TUI renders the same rows
 * as a timeline of labels and diffs instead.
 *
 * <p>Every value is a string the publisher wrote, and several are written as placeholders rather than
 * left out: an unset story point is stored as the text {@code "null"}, an absent parent as an empty
 * string, an unknown VCS user as {@code "UNKNOWN"}. Those must not reach a reader, so a value is treated
 * as missing unless it survives {@link #text}.
 */
final class ActivitySentence {

    /** {@code String.valueOf(null)} in the publisher stores this text, not an absent key. */
    private static final String NULL_TEXT = "null";

    /** The publisher's stand-in for a VCS identity it could not resolve. */
    private static final String UNKNOWN_TEXT = "UNKNOWN";

    /** Used when a row carries no actor at all, so a sentence never opens with a blank. */
    private static final String SOMEONE = "Someone";

    /**
     * Types whose sentence already states the before and after, so repeating the diff underneath would
     * say the same thing twice. Every other type shows its changes, including one added later.
     */
    private static final Set<ActivityType> DIFF_IN_SENTENCE = Set.of(
            ActivityType.ISSUE_WORKFLOW_TRANSITIONED,
            ActivityType.ISSUE_WORKFLOW_TRANSITIONED_BY_SYSTEM,
            ActivityType.ISSUE_STORY_POINT_CHANGED,
            ActivityType.ISSUE_PARENT_CHANGED);

    private ActivitySentence() {}

    static String summarize(ActivityLogResponse log) {
        Map<String, String> data = log.data() != null ? log.data() : Map.of();
        String actor = actorOf(data);

        return switch (log.type()) {
            case ISSUE_CREATED -> actor + " created the issue.";
            case ISSUE_UPDATED -> actor + " updated the issue.";
            case ISSUE_DELETED -> actor + " deleted the issue.";
            case ISSUE_RESTORED -> actor + " restored the issue.";

            case ISSUE_ASSIGNED -> assigned(actor, text(data, ASSIGNEE_DISPLAY_NAME));
            case ISSUE_UNASSIGNED -> unassigned(actor, text(data, REMOVED_ASSIGNEE_DISPLAY_NAME));

            case ISSUE_WORKFLOW_TRANSITIONED -> transition(actor, text(data, OLD_STATE), text(data, NEW_STATE));
            case ISSUE_WORKFLOW_TRANSITIONED_BY_SYSTEM -> automatedTransition(data);

            case ISSUE_STORY_POINT_CHANGED -> storyPoint(actor, text(data, OLD_POINT), text(data, NEW_POINT));
            case ISSUE_PARENT_CHANGED -> parent(actor, text(data, OLD_PARENT_KEY), text(data, NEW_PARENT_KEY));

            case ISSUE_RELATION_ADDED ->
                actor + " recorded that this issue " + relationVerb(text(data, RELATION_TYPE)) + " "
                        + text(data, TARGET_ISSUE_KEY) + ".";
            case ISSUE_RELATION_REMOVED ->
                actor + " removed the link saying this issue " + relationVerb(text(data, RELATION_TYPE)) + " "
                        + text(data, TARGET_ISSUE_KEY) + ".";

            case ISSUE_REVIEW_REQUESTED -> reviewRequested(actor, text(data, REVIEWER_COUNT));
            case ISSUE_REVIEW_SUBMITTED -> reviewSubmitted(actor, text(data, REVIEW_STATUS));
            case ISSUE_REVIEWER_ADDED -> actor + " added " + nameOr(data, REVIEWER_DISPLAY_NAME) + " as a reviewer.";
            case ISSUE_REVIEWER_REMOVED ->
                actor + " removed " + nameOr(data, REMOVED_REVIEWER_DISPLAY_NAME) + " as a reviewer.";

            case ISSUE_COMMENT_ADDED -> actor + " commented.";
            case ISSUE_COMMENT_UPDATED -> actor + " edited a comment.";
            case ISSUE_COMMENT_DELETED -> actor + " deleted a comment.";

            case ISSUE_BRANCH_CONNECTED -> branchLinked(actor, text(data, BRANCH_NAME));
            case ISSUE_VCS_CONNECTION_LINKED ->
                pullRequest(text(data, ACTOR_DISPLAY_NAME), text(data, PR_TITLE), text(data, PR_ACTION));

            case SPRINT_STARTED -> actor + " started sprint " + text(data, SPRINT_TITLE) + ".";
            case SPRINT_COMPLETED -> actor + " completed sprint " + text(data, SPRINT_TITLE) + ".";
        };
    }

    /**
     * The change lines to show under a sentence, or an empty list when the sentence already carries them.
     */
    static List<String> changeLines(ActivityLogResponse log) {
        if (DIFF_IN_SENTENCE.contains(log.type())
                || log.changes() == null
                || log.changes().isEmpty()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        // the changes arrive as a map, so order by field name for a stable read across calls
        for (Map.Entry<String, FieldChange> entry : new TreeMap<>(log.changes()).entrySet()) {
            String line = changeLine(entry.getKey(), entry.getValue());
            if (line != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    /**
     * A body change reports only that it happened: the before and after are whole documents, and printing
     * them would bury every other line under them.
     */
    private static @Nullable String changeLine(String field, FieldChange change) {
        if ("content".equals(field)) {
            return "content updated";
        }
        String from = valueOf(change.from());
        String to = valueOf(change.to());

        if (!from.isEmpty() && !to.isEmpty()) {
            return field + ": " + from + " -> " + to;
        }
        if (!to.isEmpty()) {
            return field + ": " + to;
        }
        if (!from.isEmpty()) {
            return field + ": " + from + " (cleared)";
        }
        return null; // neither side has a value; an empty diff tells a reader nothing
    }

    /**
     * Both state names are written by the publisher, so in practice both are there; phrasing around a
     * missing one costs a branch and means no row can ever render as "moved the issue from  to .".
     */
    private static String transition(String actor, String from, String to) {
        if (!from.isEmpty() && !to.isEmpty()) {
            return actor + " moved the issue from " + from + " to " + to + ".";
        }
        if (!to.isEmpty()) {
            return actor + " moved the issue to " + to + ".";
        }
        if (!from.isEmpty()) {
            return actor + " moved the issue out of " + from + ".";
        }
        return actor + " moved the issue to another state.";
    }

    private static String assigned(String actor, String assignee) {
        return assignee.isEmpty() ? actor + " assigned the issue." : actor + " assigned the issue to " + assignee + ".";
    }

    private static String unassigned(String actor, String removed) {
        return removed.isEmpty() ? actor + " unassigned the issue." : actor + " unassigned " + removed + ".";
    }

    /**
     * The automated transition is the one row with no actor name: it is published with a null actor and
     * carries the VCS identity instead, so it names the automation and credits the VCS user when known.
     */
    private static String automatedTransition(Map<String, String> data) {
        StringBuilder sentence =
                new StringBuilder(transition("Automation", text(data, OLD_STATE), text(data, NEW_STATE)));
        sentence.setLength(sentence.length() - 1); // drop the full stop; the clauses below finish the sentence

        String reason = text(data, TRIGGER_REASON);
        if (!reason.isEmpty()) {
            sentence.append(", triggered by ").append(reason);
        }
        String vcsUser = text(data, VCS_USER_NAME);
        if (!vcsUser.isEmpty() && !UNKNOWN_TEXT.equals(vcsUser)) {
            sentence.append(" from ").append(vcsUser);
        }
        return sentence.append('.').toString();
    }

    private static String storyPoint(String actor, String from, String to) {
        if (!from.isEmpty() && !to.isEmpty()) {
            return actor + " changed the story point from " + from + " to " + to + ".";
        }
        if (!to.isEmpty()) {
            return actor + " set the story point to " + to + ".";
        }
        if (!from.isEmpty()) {
            return actor + " cleared the story point, which was " + from + ".";
        }
        return actor + " changed the story point.";
    }

    private static String parent(String actor, String from, String to) {
        if (!from.isEmpty() && !to.isEmpty()) {
            return actor + " moved the issue under " + to + ", from " + from + ".";
        }
        if (!to.isEmpty()) {
            return actor + " put the issue under " + to + ".";
        }
        if (!from.isEmpty()) {
            return actor + " took the issue out from under " + from + ".";
        }
        return actor + " changed the parent issue.";
    }

    /**
     * The stored count is how many reviews were reset, not how many reviewers were named: a reviewer whose
     * review is still pending is left alone, so asking three people who have not looked yet records zero.
     * The wording says what the number means rather than treating it as an audience size.
     */
    private static String reviewRequested(String actor, String count) {
        if (count.isEmpty() || "0".equals(count)) {
            return actor + " requested a re-review, but no reviewer had given a verdict to reset.";
        }
        String reviewers = "1".equals(count) ? "1 reviewer" : count + " reviewers";
        return actor + " asked " + reviewers + " to review the issue again.";
    }

    /**
     * Deliberately passive about who acted. The stored name is the pull request's AUTHOR - the webhook's
     * {@code sender}, the account that actually opened or merged it, is parsed and then never read - so
     * saying "{name} merged it" would credit the merge to whoever wrote the branch.
     *
     * <p>TODO: return to the active voice once the publisher carries the sender. The change is tracked by
     * a TODO on {@code GithubPrPayload.toVcsDto}; until then this wording is the only true one.
     */
    private static String pullRequest(String author, String title, String prAction) {
        String subject = title.isEmpty() ? "A pull request" : "Pull request" + quoted(title);
        String by = author.isEmpty() || UNKNOWN_TEXT.equals(author) ? "" : " by " + author;

        return subject + by + " was " + prVerb(prAction) + ".";
    }

    private static String reviewSubmitted(String actor, String status) {
        return switch (status) {
            case "APPROVED" -> actor + " approved the issue.";
            case "CHANGES_REQUESTED" -> actor + " reviewed the issue and requested changes.";
            default -> actor + " submitted a review.";
        };
    }

    private static String branchLinked(String actor, String branch) {
        return branch.isEmpty() ? actor + " linked a branch." : actor + " linked the branch " + branch + ".";
    }

    /** Reads the relation from this issue's side, so the direction is unambiguous in the sentence. */
    private static String relationVerb(String relationType) {
        return switch (relationType) {
            case "BLOCKS" -> "blocks";
            case "CAUSES" -> "causes";
            case "DUPLICATES" -> "duplicates";
            case "RELEVANT" -> "relates to";
            default -> "is linked to";
        };
    }

    private static String prVerb(String prAction) {
        return switch (prAction) {
            case "OPENED" -> "opened";
            case "CLOSED" -> "closed";
            case "MERGED" -> "merged";
            case "REOPENED" -> "reopened";
            default -> "updated";
        };
    }

    private static String quoted(String title) {
        return title.isEmpty() ? "" : " \"" + title + "\"";
    }

    private static String actorOf(Map<String, String> data) {
        String actor = text(data, ACTOR_DISPLAY_NAME);
        return actor.isEmpty() || UNKNOWN_TEXT.equals(actor) ? SOMEONE : actor;
    }

    private static String nameOr(Map<String, String> data, String key) {
        String name = text(data, key);
        return name.isEmpty() ? "someone" : name;
    }

    /** A stored value, or empty when it is absent, blank, or one of the publisher's placeholder texts. */
    private static String text(Map<String, String> data, String key) {
        String value = data.get(key);
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return NULL_TEXT.equals(trimmed.toLowerCase(Locale.ROOT)) ? "" : trimmed;
    }

    private static String valueOf(@Nullable Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).trim();
        return NULL_TEXT.equals(text.toLowerCase(Locale.ROOT)) ? "" : text;
    }
}
