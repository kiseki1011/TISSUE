"""Geometric (2D) directional focus for a screen's focusable widgets.

`focus_in_direction(screen, "down")` moves focus to the nearest focusable widget in
that direction by on-screen position, so arrow / hjkl keys feel spatial on a 2D
layout (a column of fields above a row of buttons) instead of a flat Tab order.

Direction:
    - left/right stay within the same row band. If nothing focusable sits beside
      the current widget, focus doesn't move, so h/l on a vertical column is a
      no-op rather than leaping to another group.
    - up/down prefer a column-aligned target but fall back to the nearest one in
      that direction, so they cross from one row group to the next (the field
      column down into the button row).
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

    focused_region = focused.region
    focused_center_x = focused_region.x + focused_region.width / 2
    focused_center_y = focused_region.y + focused_region.height / 2
    horizontal = direction in ("left", "right")
    # A focusable scroll container (e.g. the dialog) spans the whole layout, so its
    # region would shadow every real control. Skip the focused widget's ancestors.
    ancestors = set(focused.ancestors)

    best = None
    best_score = 0.0
    for widget in chain:
        if widget is focused or widget in ancestors:
            continue
        candidate_region = widget.region
        if candidate_region.width == 0 or candidate_region.height == 0:
            continue
        candidate_center_x = candidate_region.x + candidate_region.width / 2
        candidate_center_y = candidate_region.y + candidate_region.height / 2
        if direction == "left" and candidate_center_x >= focused_center_x:
            continue
        if direction == "right" and candidate_center_x <= focused_center_x:
            continue
        if direction == "up" and candidate_center_y >= focused_center_y:
            continue
        if direction == "down" and candidate_center_y <= focused_center_y:
            continue

        if horizontal:
            if not (
                candidate_region.y < focused_region.y + focused_region.height
                and focused_region.y < candidate_region.y + candidate_region.height
            ):
                continue
            score = abs(candidate_center_x - focused_center_x) + abs(
                candidate_center_y - focused_center_y
            )
        else:
            x_overlap = (
                candidate_region.x < focused_region.x + focused_region.width
                and focused_region.x < candidate_region.x + candidate_region.width
            )
            score = abs(candidate_center_y - focused_center_y) + abs(
                candidate_center_x - focused_center_x
            )
            if not x_overlap:
                score += _OFF_AXIS_PENALTY

        if best is None or score < best_score:
            best = widget
            best_score = score

    if best is not None:
        best.focus()
