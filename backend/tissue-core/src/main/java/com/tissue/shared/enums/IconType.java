package com.tissue.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IconType {
    CIRCLE_FILLED("●"),
    CIRCLE_OUTLINE("○"),
    CIRCLE_DOT("◉"),
    CIRCLE_DOUBLE("◎"),
    CIRCLE_HALF_LEFT("◐"),
    CIRCLE_HALF_RIGHT("◑"),
    CIRCLE_LARGE_DOT("◕"),

    SQUARE_FILLED("■"),
    SQUARE_OUTLINE("□"),
    SQUARE_SMALL_FILLED("▪"),
    SQUARE_SMALL_OUTLINE("▫"),
    SQUARE_MEDIUM_FILLED("◼"),
    SQUARE_MEDIUM_OUTLINE("◻"),
    RECTANGLE_FILLED("▬"),

    TRIANGLE_UP_FILLED("▲"),
    TRIANGLE_UP_OUTLINE("△"),
    TRIANGLE_DOWN_FILLED("▼"),
    TRIANGLE_DOWN_OUTLINE("▽"),
    TRIANGLE_RIGHT_FILLED("▶"),
    TRIANGLE_RIGHT_OUTLINE("▷"),
    TRIANGLE_LEFT_FILLED("◀"),
    TRIANGLE_LEFT_OUTLINE("◁"),

    DIAMOND_FILLED("◆"),
    DIAMOND_OUTLINE("◇"),
    DIAMOND_CROSS("❖"),

    STAR_FILLED("★"),
    STAR_OUTLINE("☆"),
    STAR_4_FILLED("✦"),
    STAR_4_OUTLINE("✧"),

    CROSS_FILLED("✚"),
    CROSS_X("✕"),
    CROSS_X_HEAVY("✖"),
    PLUS("＋"),
    CIRCLE_PLUS("⊕"),
    CIRCLE_X("⊗"),
    SQUARE_PLUS("⊞"),
    SQUARE_X("⊠"),

    ARROW_RIGHT("→"),
    ARROW_LEFT("←"),
    ARROW_UP("↑"),
    ARROW_DOWN("↓"),
    ARROW_UP_RIGHT("↗"),
    ARROW_DOWN_RIGHT("↘"),
    ARROW_DOUBLE_RIGHT("⇒"),
    ARROW_DOUBLE_UP("⇑"),

    LINE_HORIZONTAL("─"),
    LINE_CROSS("┼"),
    LINE_DASHED_HORIZONTAL("╌"),
    BAR_LEFT_HALF("▌"),
    BAR_RIGHT_HALF("▐"),
    BAR_FULL("█"),
    BAR_BOTTOM_HALF("▄"),
    BAR_TOP_HALF("▀"),

    FLAG("⚑"),
    FLAG_OUTLINE("⚐"),
    CHECKBOX_CHECKED("☑"),
    CHECKBOX_UNCHECKED("☐"),
    MINUS_SQUARE("⊟"),
    MENU("≡"),
    DOTS_VERTICAL("⋮"),
    DOTS_HORIZONTAL("⋯"),
    INFINITY("∞"),
    PROHIBITED("⊘"),
    WARNING("⚠"),
    TARGET("⊙"),

    CHEVRON_LEFT_DOUBLE("《"),
    CHEVRON_RIGHT_DOUBLE("》"),
    CHEVRON_LEFT_SINGLE("‹"),
    CHEVRON_RIGHT_SINGLE("›");

    private final String symbol;

    @Override
    public String toString() {
        return symbol;
    }
}
