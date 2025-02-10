package com.tissue.api.util;

import java.util.Random;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RandomNicknameGenerator {

	private static final int NUMERIC_SUFFIX_LENGTH = 3;

	private enum Adjective {
		HAPPY, SWIFT, CLEVER, BRIGHT, CALM, WISE, KIND, BOLD, BRAVE, CREATIVE, GENTLE,
		HONEST, JOYFUL;

		private static final Random RANDOM = new Random();

		public static Adjective random() {
			return values()[RANDOM.nextInt(values().length)];
		}

		@Override
		public String toString() {
			// HAPPY -> Happy 형태로 변환
			return name().charAt(0) + name().substring(1).toLowerCase();
		}
	}

	private enum Animal {
		PANDA, EAGLE, TIGER, DOLPHIN, LION, WOLF, BEAR, FOX, DEER, ELEPHANT, FALCON,
		GAZELLE, HAWK, IGUANA, JAGUAR, KOALA, PENGUIN, RABBIT, SEAL, TURTLE, UNICORN,
		WHALE, ZEBRA;

		private static final Random RANDOM = new Random();

		public static Animal random() {
			return values()[RANDOM.nextInt(values().length)];
		}

		@Override
		public String toString() {
			// PANDA -> Panda 형태로 변환
			return name().charAt(0) + name().substring(1).toLowerCase();
		}
	}

	public String generateNickname() {
		String randomNumber = generateRandomNumber();
		return Adjective.random().toString() + Animal.random().toString() + randomNumber;
	}

	private String generateRandomNumber() {
		StringBuilder sb = new StringBuilder();
		Random random = new Random();

		for (int i = 0; i < NUMERIC_SUFFIX_LENGTH; i++) {
			sb.append(random.nextInt(10));
		}

		return sb.toString();
	}
}
