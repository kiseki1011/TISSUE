package com.tissue.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IconType {
    CIRCLE_FILLED("●"),
    CIRCLE_OUTLINE("○"),
    CIRCLE_DOT("◉"),

    SQUARE_FILLED("■"),
    SQUARE_OUTLINE("□"),

    TRIANGLE_UP_FILLED("▲"),
    TRIANGLE_DOWN_FILLED("▼"),

    DIAMOND_FILLED("◆"),
    DIAMOND_OUTLINE("◇"),

    STAR_FILLED("★"),
    STAR_OUTLINE("☆"),

    FLAG("⚑"),
    WARNING("⚠"),
    TARGET("⊙"),
    PROHIBITED("⊘"),

    CHECKBOX_CHECKED("☑"),
    CHECKBOX_UNCHECKED("☐"),

    ARROW_RIGHT("→"),
    ARROW_UP("↑"),
    ARROW_DOWN("↓"),

    CHEVRON_LEFT_DOUBLE("《"),
    CHEVRON_RIGHT_DOUBLE("》"),

    MENU("≡"),
    DOTS_VERTICAL("⋮"),
    DOTS_HORIZONTAL("⋯");

    private final String symbol;
}
