"""Shared constants for the home (dashboard) screen and its area modules."""

_SEARCH_SIZE = 20

# Live search ([1] Searched Items): minimum keyword length (after the
# /project: /issue: prefix) before hitting the API, and the debounce
# (seconds) we wait after the last keystroke before firing.
_MIN_QUERY_LEN = 2
_SEARCH_DEBOUNCE = 0.2

# Key column widths (chars); keys longer than this are clipped with a "…".
_PROJECT_KEY_WIDTH = 11
_ISSUE_KEY_WIDTH = 14

# Search-bar command prefixes → search kind.
_SEARCH_PREFIXES = {"/project:": "project", "/issue:": "issue"}
