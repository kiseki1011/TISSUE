"""Module constants for the ProjectHome hub."""

from __future__ import annotations

# The [1] box cycles through these list views; CTRL+T advances to the next one
# (wrapping around). `_VIEW_LABELS` gives each view's display name for the box
# border title (current view + the next view it hints).
# Workflow state categories that count as "open" (not yet terminal): used to list
# the issues a sprint can still pull in. COMPLETED / ABORTED are the terminal ones.
_OPEN_STATE_CATEGORIES: list[str] = ["INITIAL", "ACTIVE"]

# Live-search debounce: the search only fires this long after typing pauses, so a
# burst of keystrokes triggers one load, not one per key (mirrors the dashboard).
_SEARCH_DEBOUNCE = 0.2

_VIEW_CYCLE: tuple[str, ...] = ("issues", "sprints", "members")
_VIEW_LABELS: dict[str, str] = {
    "issues": "Issues",
    "sprints": "Sprints",
    "members": "Members",
}

# The [3] box toggles (CTRL+T while focused) between these two modes.
_AGENT_MODE_LABELS: dict[str, str] = {
    "work": "Agent Work",
    "reviews": "Requested Reviews",
}

# Sprint status is a fixed enum (no server-defined colour), so the TUI fixes one
# per state, used as the chip *background* (mirrors `PRIORITY_VAR` in
# `tissue.widgets.issue_render`).
_SPRINT_STATUS_VAR: dict[str, str] = {
    "PLANNING": "primary",
    "ACTIVE": "success",
    "COMPLETED": "secondary",
    "CANCELLED": "error",
}

# Skipped `data` keys on an activity event: the per-event context
# (actor/issue/project) shown elsewhere, plus the raw old*/new* of diff events —
# those duplicate the `changes` FieldChange (kept as a single "before → after").
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
