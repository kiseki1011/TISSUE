"""Shared constants for the home (dashboard) screen and its area modules."""

_SEARCH_SIZE = 20

# Live search for [1] Searched Items.
#   - _MIN_QUERY_LEN -> shortest keyword (after the /project: or /issue: prefix)
#     that triggers an API call
#   - _SEARCH_DEBOUNCE -> seconds to wait after the last keystroke before firing
_MIN_QUERY_LEN = 2
_SEARCH_DEBOUNCE = 0.2

# Key column widths in chars. Keys longer than this are clipped with a "…".
_PROJECT_KEY_WIDTH = 11
_ISSUE_KEY_WIDTH = 15

# Search-bar command prefixes -> search kind.
_SEARCH_PREFIXES = {"/project:": "project", "/issue:": "issue"}
