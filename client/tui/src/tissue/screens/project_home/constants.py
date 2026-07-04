from __future__ import annotations

# Not-finished categories, the issues a sprint can still pull in.
_OPEN_STATE_CATEGORIES: list[str] = ["INITIAL", "ACTIVE"]

# Wait a moment so fast typing triggers one load, not one per key.
_SEARCH_DEBOUNCE = 0.2
_DETAIL_PREFETCH_BEFORE = 2
_DETAIL_PREFETCH_AFTER = 3

# Labels feed the [1] box title (all contexts, the active one bold).
_VIEW_CYCLE: tuple[str, ...] = (
    "issues",
    "agent",
    "reviews",
    "sprints",
    "members",
)
_VIEW_LABELS: dict[str, str] = {
    "issues": "Issues",
    "sprints": "Sprints",
    "members": "Members",
    "agent": "Agent's Work",
    "reviews": "Reviews",
}

# Views that list issues, so the issue actions (edit/assign/transition/add to
# sprint, new issue) apply and the [2] detail shows an issue.
_ISSUE_VIEWS: frozenset[str] = frozenset({"issues", "agent", "reviews"})

# The server gives no color for sprint status, so sets one per state,
# used as the chip background.
_SPRINT_STATUS_VAR: dict[str, str] = {
    "PLANNING": "primary",
    "ACTIVE": "success",
    "COMPLETED": "secondary",
    "CANCELLED": "error",
}

# Activity `data` keys to skip.
_ACTIVITY_DATA_SKIP = frozenset(
    {
        "issueKey",
        "actorName",
        "actorEmail",
        "projectKey",
        "oldState",
        "newState",
        "oldPoint",
        "newPoint",
        "oldParent",
        "newParent",
    }
)
