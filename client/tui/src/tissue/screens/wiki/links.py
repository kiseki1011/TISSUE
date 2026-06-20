"""Parse Markdown link hrefs: internal `wiki:`/`issue:`/`project:` schemes vs
plain web links (`http`/`https`/`mailto`)."""

from __future__ import annotations

from urllib.parse import urlparse

# Internal link schemes: [text](wiki:ID) / [text](issue:KEY) / [text](project:KEY).
_LINK_SCHEMES = ("wiki", "issue", "project")

# External schemes handed to the OS browser / mail client.
_WEB_LINK_SCHEMES = ("http", "https", "mailto")


def _parse_link(href: str) -> tuple[str, str] | None:
    """Split an internal `scheme:value` link into (scheme, value); None if not one."""
    for scheme in _LINK_SCHEMES:
        prefix = f"{scheme}:"
        if href.startswith(prefix):
            return scheme, href[len(prefix) :]
    return None


def _web_url(href: str) -> str | None:
    """`href` if it's an external link to open in the browser/mail client
    (http / https / mailto); None otherwise."""
    return href if urlparse(href).scheme.lower() in _WEB_LINK_SCHEMES else None
