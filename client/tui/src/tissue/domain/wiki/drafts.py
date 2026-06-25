from __future__ import annotations

import logging
import os
import re
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path

import yaml

log = logging.getLogger(__name__)

# Leading `---` line, the YAML block, a closing `---` line, then the body.
_FRONTMATTER_RE = re.compile(r"^---\n(.*?)\n---\n?(.*)$", re.DOTALL)
# Characters unsafe in a filename across platforms, plus control chars.
_UNSAFE = re.compile(r'[<>:"/\\|?*\x00-\x1f]')
_WHITESPACE = re.compile(r"\s+")
_TIMESTAMP_FMT = "%Y%m%d-%H%M%S"
_SYNCED_SUBDIR = "synced"
_SLUG_MAX = 40


def _slug(title: str) -> str:
    """Build a filesystem-safe stem from a title.

    Leading and trailing ``. _ -`` are stripped so the name can't look like a
    hidden file or a command-line flag. An all-unsafe or empty title falls back
    to a constant so we never produce a bare ``.md``.
    """
    slug = _UNSAFE.sub("", title)
    slug = _WHITESPACE.sub("_", slug).strip("._-")
    if len(slug) > _SLUG_MAX:
        slug = slug[:_SLUG_MAX].rstrip("._-")
    return slug or "untitled"


def _split_frontmatter(text: str) -> tuple[dict[str, object], str]:
    """Split a document into a frontmatter dict and the body.

    When there is no frontmatter, or it isn't a dict, returns an empty dict and
    the original text unchanged.
    """
    match = _FRONTMATTER_RE.match(text)
    if match is None:
        return {}, text
    try:
        data = yaml.safe_load(match.group(1))
    except yaml.YAMLError as error:
        log.warning("malformed draft frontmatter: %s", error)
        return {}, text
    if not isinstance(data, dict):
        return {}, text
    return data, match.group(2)


@dataclass
class Draft:
    """An offline wiki draft, a local Markdown file with YAML frontmatter.

    The frontmatter carries the title and tags, the body is the Markdown
    content. ``path`` is the file it loads from and saves to. A None path means
    it has never been written to disk yet.
    """

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
            [str(tag).strip() for tag in raw_tags if str(tag).strip()]
            if isinstance(raw_tags, list)
            else []
        )
        return cls(title=title, tags=tags, body=body.strip("\n"), path=path)

    def to_text(self) -> str:
        """Serialize to frontmatter + body Markdown."""
        meta = {"title": self.title, "tags": self.tags}
        frontmatter = yaml.safe_dump(
            meta, allow_unicode=True, sort_keys=False, default_flow_style=False
        )
        body = self.body.strip()
        return (
            f"---\n{frontmatter}---\n\n{body}\n" if body else f"---\n{frontmatter}---\n"
        )

    def modified_at(self) -> datetime | None:
        if self.path is None:
            return None
        try:
            return datetime.fromtimestamp(self.path.stat().st_mtime)
        except OSError:
            return None


class DraftStore:
    """Read and write offline drafts under a root folder.

    Top-level ``.md`` files are the open drafts. The ``synced/`` subfolder holds
    drafts that have already been published, so they're kept but no longer
    listed as open.
    """

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
        for draft_path in sorted(self.root.glob("*.md")):
            if not draft_path.is_file():
                continue
            try:
                drafts.append(Draft.from_file(draft_path))
            except OSError as error:
                log.warning("skipping unreadable draft %s: %s", draft_path, error)
        drafts.sort(key=_mtime, reverse=True)
        return drafts

    def save(self, draft: Draft) -> Path:
        """Write the draft and update its ``path`` to the written file.

        A draft with no ``path`` gets a fresh ``{slug}_{timestamp}.md``. An
        existing one is overwritten in place.
        """
        self.root.mkdir(parents=True, exist_ok=True)
        path = draft.path or self._new_path(draft.title)
        _atomic_write(path, draft.to_text())
        draft.path = path
        return path

    def mark_synced(self, path: Path) -> Path | None:
        """Move a published draft into the ``synced/`` subfolder.

        Returns the new path, or None if the source no longer exists.
        """
        if not path.is_file():
            return None
        self.synced_dir.mkdir(parents=True, exist_ok=True)
        dest = self.synced_dir / path.name
        os.replace(path, dest)  # atomic within one filesystem, overwrites
        return dest

    def _new_path(self, title: str) -> Path:
        base = f"{_slug(title)}_{datetime.now().strftime(_TIMESTAMP_FMT)}"
        path = self.root / f"{base}.md"
        counter = 1
        while path.exists():  # two drafts in the same second need distinct names
            path = self.root / f"{base}-{counter}.md"
            counter += 1
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
