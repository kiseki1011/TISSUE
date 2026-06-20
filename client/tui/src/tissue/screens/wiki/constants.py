"""Shared constants for the wiki screen and its area modules."""

# Shown (italic, centred) in the reading pane before any document is opened.
_PLACEHOLDER_TEXT = "Select a document"

# Tree titles longer than this are clipped with a trailing "…".
_TITLE_LIMIT = 24

# Max search results fetched at once.
_SEARCH_SIZE = 50

# Live search: minimum query length, and the debounce (seconds) we wait after
# the last keystroke before firing so we don't hit the API on every key.
_MIN_QUERY_LEN = 2
_SEARCH_DEBOUNCE = 0.2

# Sentinel for the version <Select>'s "Current" option (the live document).
# Snapshot ids are positive, so 0 never collides with a real snapshot.
_CURRENT_VERSION = 0

# Sentinel for the authoring parent <Select>'s "new root document" option.
# Document ids are positive, so 0 never collides with one.
_ROOT_PARENT = 0

# Version-bump options for the edit-mode picker (value = server's
# SemanticUpdateType enum name). PATCH is the default — most edits are small.
_VERSION_BUMP_OPTIONS = [
    ("Patch (x.y.+1)", "PATCH"),
    ("Minor (x.+1.0)", "MINOR"),
    ("Major (+1.0.0)", "MAJOR"),
]
_DEFAULT_VERSION_BUMP = "PATCH"

# The parent-document link in the meta header is clipped to this many chars.
_PARENT_TITLE_LIMIT = 20

# A document carries at most 5 tags (server-enforced); re-capped on publish.
_MAX_TAGS = 5

# Server limits on a new document (CreateDocumentRequest): checked client-side
# so over-limit input gets a clear message instead of a generic server error.
_TITLE_MAX = 200
_CONTENT_MAX = 100_000
