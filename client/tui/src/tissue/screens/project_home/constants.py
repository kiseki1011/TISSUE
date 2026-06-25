from __future__ import annotations

# Not-finished categories, the issues a sprint can still pull in.
_OPEN_STATE_CATEGORIES: list[str] = ["INITIAL", "ACTIVE"]

# Wait a moment so fast typing triggers one load, not one per key.
_SEARCH_DEBOUNCE = 0.2

# Labels feed the [1] box title (current view plus the next one it hints).
_VIEW_CYCLE: tuple[str, ...] = ("issues", "sprints", "members")
_VIEW_LABELS: dict[str, str] = {
    "issues": "Issues",
    "sprints": "Sprints",
    "members": "Members",
}

_AGENT_MODE_LABELS: dict[str, str] = {
    "work": "Agent Work",
    "reviews": "Requested Reviews",
}

# The server gives no color for sprint status, so the TUI sets one per state,
# used as the chip *background*, the same way as `PRIORITY_VAR` in
# `issue_render`.
_SPRINT_STATUS_VAR: dict[str, str] = {
    "PLANNING": "primary",
    "ACTIVE": "success",
    "COMPLETED": "secondary",
    "CANCELLED": "error",
}

# Activity `data` keys to skip. They are shown elsewhere, plus the raw
# old*/new* pairs just repeat `changes` (already shown as one "before → after").
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
