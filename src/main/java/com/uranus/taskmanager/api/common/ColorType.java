package com.uranus.taskmanager.api.common;

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

	RED("#FF5733", "Red"),
	PINK("#FF69B4", "Pink"),
	ORANGE("#FF8C00", "Orange"),
	YELLOW("#FFD700", "Yellow"),
	LIGHT_YELLOW("#FFFACD", "Light Yellow"),
	LIME("#32CD32", "Lime"),
	GREEN("#008000", "Green"),
	MINT("#98FF98", "Mint"),
	TEAL("#008080", "Teal"),
	CYAN("#00FFFF", "Cyan"),
	LIGHT_BLUE("#ADD8E6", "Light Blue"),
	BLUE("#0000FF", "Blue"),
	NAVY("#000080", "Navy"),
	INDIGO("#4B0082", "Indigo"),
	PURPLE("#800080", "Purple"),
	VIOLET("#EE82EE", "Violet"),
	MAGENTA("#FF00FF", "Magenta"),
	BROWN("#A52A2A", "Brown"),
	TAN("#D2B48C", "Tan"),
	OLIVE("#808000", "Olive"),
	GOLD("#FFD700", "Gold"),
	SILVER("#C0C0C0", "Silver"),
	GRAY("#808080", "Gray"),
	BLACK("#000000", "Black");

	private final String hexCode;
	private final String displayName;

	private static final Random RANDOM = new Random();

	public static ColorType getRandomUnusedColor(Set<ColorType> usedColors) {
		List<ColorType> availableColors = Arrays.stream(ColorType.values())
			.filter(color -> !usedColors.contains(color))
			.toList();

		if (availableColors.isEmpty()) {
			availableColors = Arrays.asList(ColorType.values());
		}

		return availableColors.get(RANDOM.nextInt(availableColors.size()));
	}
}
