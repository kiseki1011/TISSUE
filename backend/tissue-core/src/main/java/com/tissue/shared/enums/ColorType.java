package com.tissue.shared.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum ColorType {
    // ANSI 16 colors
    BLACK("#000000", "Black"),
    RED("#CC0000", "Red"),
    GREEN("#00CC00", "Green"),
    YELLOW("#CCCC00", "Yellow"),
    BLUE("#0000CC", "Blue"),
    MAGENTA("#CC00CC", "Magenta"),
    CYAN("#00CCCC", "Cyan"),
    WHITE("#CCCCCC", "White"),
    GRAY("#808080", "Gray"),
    BRIGHT_RED("#FF0000", "Bright Red"),
    BRIGHT_GREEN("#00FF00", "Bright Green"),
    BRIGHT_YELLOW("#FFFF00", "Bright Yellow"),
    BRIGHT_BLUE("#0000FF", "Bright Blue"),
    BRIGHT_MAGENTA("#FF00FF", "Bright Magenta"),
    BRIGHT_CYAN("#00FFFF", "Bright Cyan"),
    BRIGHT_WHITE("#FFFFFF", "Bright White"),

    // Extended colors
    PINK("#FF69B4", "Pink"),
    ORANGE("#FF8C00", "Orange"),
    LIME("#32CD32", "Lime"),
    TEAL("#008080", "Teal"),
    NAVY("#000080", "Navy"),
    INDIGO("#4B0082", "Indigo"),
    PURPLE("#800080", "Purple"),
    BROWN("#8B4513", "Brown"),
    TAN("#D2B48C", "Tan"),
    OLIVE("#808000", "Olive");

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
