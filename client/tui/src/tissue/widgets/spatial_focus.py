"""Geometric (2D) directional focus for a screen's focusable widgets.

`focus_in_direction(screen, "down")` moves focus to the nearest focusable widget in
that direction by on-screen position, so arrow / hjkl keys feel spatial on a 2D
layout (a column of fields above a row of buttons) instead of a flat Tab order.

- left/right stay within the same row band: if nothing focusable sits beside the
  current widget, focus doesn't move (so h/l on a vertical column is a no-op rather
  than leaping to another group).
- up/down prefer a column-aligned target but fall back to the nearest one in that
  direction, so they cross from one row group to the next (e.g. the field column
  down into the button row).
"""

from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from textual.screen import Screen

_OFF_AXIS_PENALTY = 1000.0


def focus_in_direction(screen: Screen, direction: str) -> None:
    chain = list(screen.focus_chain)
    if not chain:
        return
    focused = screen.focused
    if focused is None or focused not in chain:
        chain[0].focus()
        return

    fr = focused.region
    fcx = fr.x + fr.width / 2
    fcy = fr.y + fr.height / 2
    horizontal = direction in ("left", "right")
    # A focusable scroll container (e.g. the dialog) spans the whole layout, so its
    # region would shadow every real control — skip the focused widget's ancestors.
    ancestors = set(focused.ancestors)

    best = None
    best_score = 0.0
    for widget in chain:
        if widget is focused or widget in ancestors:
            continue
        r = widget.region
        if r.width == 0 or r.height == 0:
            continue
        cx = r.x + r.width / 2
        cy = r.y + r.height / 2
        # Must lie strictly in the requested direction.
        if direction == "left" and cx >= fcx:
            continue
        if direction == "right" and cx <= fcx:
            continue
        if direction == "up" and cy >= fcy:
            continue
        if direction == "down" and cy <= fcy:
            continue

        if horizontal:
            # Only consider widgets whose row overlaps the current one.
            if not (r.y < fr.y + fr.height and fr.y < r.y + r.height):
                continue
            score = abs(cx - fcx) + abs(cy - fcy)
        else:
            x_overlap = r.x < fr.x + fr.width and fr.x < r.x + r.width
            score = abs(cy - fcy) + abs(cx - fcx)
            if not x_overlap:
                score += _OFF_AXIS_PENALTY

        if best is None or score < best_score:
            best = widget
            best_score = score

    if best is not None:
        best.focus()
