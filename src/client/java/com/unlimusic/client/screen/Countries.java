package com.unlimusic.client.screen;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;

/**
 * Country names for the radio search: the player types "Portugal" (or "port", or "PT") and Radio Browser gets
 * "PT". The names come from Java's own list, in the game's language, so nothing is downloaded and every
 * language the game offers gets them translated for free.
 */
final class Countries {
	private static final Set<String> CODES = Set.of(Locale.getISOCountries());
	private static final String FALLBACK = "PT";

	private Countries() {
	}

	/** The country of the game's language (pt_pt → PT, en_us → US), or PT when the language names none. */
	static String defaultCode() {
		String region = gameLocale().getCountry();
		return CODES.contains(region) ? region : FALLBACK;
	}

	/** "Portugal" for PT, in the game's language; the code itself when Java has no name for it. */
	static String name(String code) {
		String name = Locale.of("", code).getDisplayCountry(gameLocale());
		return name.isBlank() ? code : name;
	}

	/**
	 * The code for what the player typed, or null when nothing matches: a two-letter code, a full name, or the
	 * start of one ("port"). Names are compared without case or accents ("espanha", "Mexico"), and English names
	 * work in every language.
	 */
	static String find(String typed) {
		String text = normalize(typed);
		if (text.isEmpty()) {
			return null;
		}
		String upper = typed.strip().toUpperCase(Locale.ROOT);
		if (upper.length() == 2 && CODES.contains(upper)) {
			return upper;
		}
		List<String> byName = sortedByName();
		for (String code : byName) {
			if (normalize(name(code)).equals(text) || normalize(englishName(code)).equals(text)) {
				return code;
			}
		}
		for (String code : byName) {
			if (normalize(name(code)).startsWith(text) || normalize(englishName(code)).startsWith(text)) {
				return code;
			}
		}
		return null;
	}

	/**
	 * What would complete {@code typed} into a country's name, shown in grey after the cursor ("port" → "ugal"),
	 * or null. Only names in the game's language, since the rest has to continue exactly what was typed.
	 */
	static String completion(String typed) {
		String text = normalize(typed);
		if (text.isEmpty()) {
			return null;
		}
		for (String code : sortedByName()) {
			String name = name(code);
			// Removing accents keeps the length, so the typed part and the name line up letter for letter.
			if (name.length() > typed.length() && normalize(name).startsWith(text)) {
				return name.substring(typed.length());
			}
		}
		return null;
	}

	/**
	 * Every code, ordered by its name in the game's language, except that the player's own country comes first:
	 * in Portuguese "port" would otherwise find Porto Rico before Portugal.
	 */
	private static List<String> sortedByName() {
		String own = defaultCode();
		return Arrays.stream(Locale.getISOCountries())
				.sorted(Comparator.comparing((String code) -> !code.equals(own)).thenComparing(code -> normalize(name(code))))
				.toList();
	}

	private static String englishName(String code) {
		return Locale.of("", code).getDisplayCountry(Locale.ENGLISH);
	}

	/** Lower case without accents: "Áustria" → "austria". */
	private static String normalize(String text) {
		// NFD splits "á" into "a" plus a combining accent (\p{M}), which is then dropped.
		return Normalizer.normalize(text.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
	}

	/** The game's language as a Java locale: "pt_pt" → pt-PT. */
	private static Locale gameLocale() {
		return Locale.forLanguageTag(Minecraft.getInstance().getLanguageManager().getSelected().replace('_', '-'));
	}
}
