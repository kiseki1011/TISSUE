"""Pure helpers that build Rich `Text` for the wiki screen (tag pills, tree
labels, search-result rows). No DOM or `self`, so they're easy to test."""

from __future__ import annotations

from rich.text import Text

from tissue.api.generated.models.wiki_document_search_result import (
    WikiDocumentSearchResult,
)
from tissue.screens.wiki.constants import _TITLE_LIMIT
from tissue.screens.wiki.tag_colors import tag_chip_style


def _tags_text(tags: list[tuple[str, str | None]]) -> Text:
    """`(name, color)` tags as one line of solid pills, one space apart.
    Empty → a dim dash."""
    if not tags:
        return Text("-", style="dim")
    text = Text()
    for i, (name, color) in enumerate(tags):
        if i:
            text.append(" ")  # gap between pills
        style = tag_chip_style(color)
        if style:
            text.append(f" {name} ", style=style)
        else:  # no colour known — fall back to plain text
            text.append(name)
    return text


def _label(title: str | None) -> Text:
    """Tree/list label: the title, clipped with a trailing "…" past the limit."""
    text = title or "Untitled"
    if len(text) > _TITLE_LIMIT:
        text = text[:_TITLE_LIMIT] + "…"
    return Text(text)


def _append_highlighted(
    text: Text, content: str, keyword: str, kw_style: str, base_style: str
) -> None:
    """Append `content`, styling every case-insensitive `keyword` occurrence
    with `kw_style` and the surrounding text with `base_style`."""
    if not keyword:
        text.append(content, style=base_style)
        return
    low = content.casefold()
    kl = keyword.casefold()
    i = 0
    while True:
        j = low.find(kl, i)
        if j == -1:
            text.append(content[i:], style=base_style)
            return
        if j > i:
            text.append(content[i:j], style=base_style)
        text.append(content[j : j + len(keyword)], style=kw_style)
        i = j + len(keyword)


def _build_snippet(snippet: str, keyword: str, kw_style: str) -> Text | None:
    """~30 chars before / 50 after the keyword (whitespace collapsed, no
    markdown). Keyword is bold on a primary background; the rest is dim."""
    flat = " ".join(snippet.split())
    if not flat:
        return None
    dim_style = "dim"
    text = Text()
    idx = flat.casefold().find(keyword.casefold())
    if idx == -1:
        # Keyword not literally present (e.g. a stemmed FTS match) — just dim the
        # start so there's still a preview.
        text.append(flat[:80], style=dim_style)
        if len(flat) > 80:
            text.append("…", style=dim_style)
        return text
    start = max(0, idx - 30)
    end = idx + len(keyword) + 50
    if start > 0:
        text.append("…", style=dim_style)
    _append_highlighted(text, flat[start:end], keyword, kw_style, dim_style)
    if end < len(flat):
        text.append("…", style=dim_style)
    return text


def _build_result_text(
    result: WikiDocumentSearchResult, *, keyword: str, primary: str | None
) -> Text:
    """One search result: `📄 Title` (keyword highlighted) plus a content-snippet
    line when the keyword matched the body but not the title."""
    title = (result.title or "").strip() or "Untitled"
    kw_style = f"bold on {primary}" if primary else "bold reverse"
    text = Text()
    text.append("📄 ")
    _append_highlighted(text, _label(title).plain, keyword, kw_style, "")
    if keyword and keyword.casefold() not in title.casefold():
        snippet = _build_snippet(result.content_snippet or "", keyword, kw_style)
        if snippet is not None and snippet.plain:
            text.append("\n")
            text.append_text(snippet)
    return text
