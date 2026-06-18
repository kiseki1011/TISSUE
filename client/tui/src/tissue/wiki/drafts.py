"""Offline wiki drafts: local Markdown files with YAML frontmatter (title + tags).

A draft is a `.md` file in the drafts folder whose frontmatter carries the
title and tags and whose body is the Markdown content:

    ---
    title: My New Page
    tags:
    - guide
    - onboarding
    ---

    # My New Page
    ...

Files are named ``{slug}_{YYYYMMDD-HHMMSS}.md``. A draft only becomes a real
wiki document when explicitly saved (published) via the API; on success the
local file is moved into a ``synced/`` subfolder so it's kept but no longer
listed as an open draft.
"""

from __future__ import annotations

import logging
import os
import re
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path

import yaml

log = logging.getLogger(__name__)

# Closing/opening frontmatter fence: leading `---` line, the YAML block, then a
# closing `---` line; the rest (after an optional newline) is the body.
_FRONTMATTER_RE = re.compile(r"^---\n(.*?)\n---\n?(.*)$", re.DOTALL)
# Characters unsafe in a filename across platforms (plus control chars).
_UNSAFE = re.compile(r'[<>:"/\\|?*\x00-\x1f]')
_WHITESPACE = re.compile(r"\s+")
_TIMESTAMP_FMT = "%Y%m%d-%H%M%S"
_SYNCED_SUBDIR = "synced"
_SLUG_MAX = 40


def _slug(title: str) -> str:
    """A filesystem-safe stem from a title (unsafe chars dropped, spaces → _).

    Leading/trailing ``. _ -`` are stripped so the name can't look like a hidden
    file or a command-line flag; an all-unsafe/empty title falls back to a
    constant so we never produce a bare ``.md``.
    """
    s = _UNSAFE.sub("", title)
    s = _WHITESPACE.sub("_", s).strip("._-")
    if len(s) > _SLUG_MAX:
        s = s[:_SLUG_MAX].rstrip("._-")
    return s or "untitled"


def _split_frontmatter(text: str) -> tuple[dict[str, object], str]:
    """Split a document into (frontmatter dict, body). No/!dict frontmatter →
    ({}, original text)."""
    match = _FRONTMATTER_RE.match(text)
    if match is None:
        return {}, text
    try:
        data = yaml.safe_load(match.group(1))
    except yaml.YAMLError as e:
        log.warning("malformed draft frontmatter: %s", e)
        return {}, text
    if not isinstance(data, dict):
        return {}, text
    return data, match.group(2)


@dataclass
class Draft:
    """An offline wiki draft. ``path`` is the file it loads from / saves to;
    None means it has never been written to disk yet."""

    title: str
    tags: list[str] = field(default_factory=list)
    body: str = ""
    path: Path | None = None

    @classmethod
    def from_file(cls, path: Path) -> Draft:
        """Load a draft from a ``.md`` file (raises OSError on read failure)."""
        meta, body = _split_frontmatter(path.read_text(encoding="utf-8"))
        title = str(meta.get("title") or "").strip() or path.stem
        raw_tags = meta.get("tags")
        tags = (
            [str(t).strip() for t in raw_tags if str(t).strip()]
            if isinstance(raw_tags, list)
            else []
        )
        return cls(title=title, tags=tags, body=body.strip("\n"), path=path)

    def to_text(self) -> str:
        """Serialize to frontmatter + body Markdown."""
        meta = {"title": self.title, "tags": self.tags}
        fm = yaml.safe_dump(
            meta, allow_unicode=True, sort_keys=False, default_flow_style=False
        )
        body = self.body.strip()
        return f"---\n{fm}---\n\n{body}\n" if body else f"---\n{fm}---\n"

    def modified_at(self) -> datetime | None:
        if self.path is None:
            return None
        try:
            return datetime.fromtimestamp(self.path.stat().st_mtime)
        except OSError:
            return None


class DraftStore:
    """Read/write offline drafts under a root folder. Top-level ``.md`` files are
    the open drafts; the ``synced/`` subfolder holds drafts already published."""

    def __init__(self, root: Path) -> None:
        self.root = root

    @property
    def synced_dir(self) -> Path:
        return self.root / _SYNCED_SUBDIR

    def list_drafts(self) -> list[Draft]:
        """Open drafts (top-level only, never the synced subfolder), newest first.

        Unreadable files are skipped (logged), so one bad file can't break the
        whole list.
        """
        if not self.root.is_dir():
            return []
        drafts: list[Draft] = []
        for p in sorted(self.root.glob("*.md")):
            if not p.is_file():
                continue
            try:
                drafts.append(Draft.from_file(p))
            except OSError as e:
                log.warning("skipping unreadable draft %s: %s", p, e)
        drafts.sort(key=_mtime, reverse=True)
        return drafts

    def save(self, draft: Draft) -> Path:
        """Write the draft. A draft with no ``path`` gets a fresh
        ``{slug}_{timestamp}.md``; an existing one is overwritten in place. The
        draft's ``path`` is updated to the written file."""
        self.root.mkdir(parents=True, exist_ok=True)
        path = draft.path or self._new_path(draft.title)
        _atomic_write(path, draft.to_text())
        draft.path = path
        return path

    def mark_synced(self, path: Path) -> Path | None:
        """Move a published draft into the ``synced/`` subfolder. Returns the new
        path, or None if the source no longer exists."""
        if not path.is_file():
            return None
        self.synced_dir.mkdir(parents=True, exist_ok=True)
        dest = self.synced_dir / path.name
        os.replace(path, dest)  # atomic within the same filesystem; overwrites
        return dest

    def _new_path(self, title: str) -> Path:
        base = f"{_slug(title)}_{datetime.now().strftime(_TIMESTAMP_FMT)}"
        path = self.root / f"{base}.md"
        i = 1
        while path.exists():  # same-second collision: disambiguate
            path = self.root / f"{base}-{i}.md"
            i += 1
        return path


def _mtime(draft: Draft) -> float:
    if draft.path is None:
        return 0.0
    try:
        return draft.path.stat().st_mtime
    except OSError:
        return 0.0


def _atomic_write(path: Path, text: str) -> None:
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_text(text, encoding="utf-8")
    os.replace(tmp, path)
