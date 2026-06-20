package com.tissue.shared.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Colors based from Textual's
 * <a href="https://textual.textualize.io/api/color/#textual.color.Color">named colors</a>.
 *
 * <p>All ANSI colors are provided, other colors are partially provided.
 * Using the HexCode and display name will not be needed in most cases. Let the client (Textual) render
 * the color based on the enum value.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum ColorType {
    // ANSI 16 colors
    ANSI_BLACK("#000000", "ANSI Black"),
    ANSI_RED("#800000", "ANSI Red"),
    ANSI_GREEN("#008000", "ANSI Green"),
    ANSI_YELLOW("#808000", "ANSI Yellow"),
    ANSI_BLUE("#000080", "ANSI Blue"),
    ANSI_MAGENTA("#800080", "ANSI Magenta"),
    ANSI_CYAN("#008080", "ANSI Cyan"),
    ANSI_WHITE("#C0C0C0", "ANSI White"),
    ANSI_BRIGHT_BLACK("#808080", "ANSI Gray"),
    ANSI_BRIGHT_RED("#FF0000", "ANSI Bright Red"),
    ANSI_BRIGHT_GREEN("#00FF00", "ANSI Bright Green"),
    ANSI_BRIGHT_YELLOW("#FFFF00", "ANSI Bright Yellow"),
    ANSI_BRIGHT_BLUE("#0000FF", "ANSI Bright Blue"),
    ANSI_BRIGHT_MAGENTA("#FF00FF", "ANSI Bright Magenta"),
    ANSI_BRIGHT_CYAN("#00FFFF", "ANSI Bright Cyan"),
    ANSI_BRIGHT_WHITE("#FFFFFF", "ANSI Bright White"),

    // Extended colors
    PINK("#FFC0CB", "Pink"),
    MAROON("#800000", "Maroon"),
    RED("#FF0000", "Red"),
    ORANGERED("#FF4500", "Orange Red"),
    DARKORANGE("FF8C00", "Dark Orange"),
    LIMEGREEN("#32CD32", "Lime Green"),
    LIGHTGREEN("90EE90", "Light Green"),
    LIGHTYELLOW("#FFFFE0", "Light Yellow"),
    MEDIUMBLUE("#0000CD", "Medium Blue"),
    MIDNIGHTBLUE("#191970", "Midnight Blue"),
    INDIGO("#4B0082", "Indigo"),
    MAGENTA("#FF00FF", "Magenta"),
    BROWN("#A52A2A", "Brown"),
    TAN("#D2B48C", "Tan");

    private static final Random RANDOM = new Random();
    private final String hexCode;
    private final String displayName;

    public static ColorType getRandomUnusedColor(Set<ColorType> usedColors) {
        List<ColorType> availableColors = Arrays.stream(ColorType.values())
                .filter(color -> !usedColors.contains(color))
                .toList();

        if (availableColors.isEmpty()) {
            availableColors = Arrays.asList(ColorType.values());
        }

        return availableColors.get(RANDOM.nextInt(availableColors.size()));
    }

    public static ColorType getRandomColor() {
        return ColorType.values()[RANDOM.nextInt(ColorType.values().length)];
    }
}
