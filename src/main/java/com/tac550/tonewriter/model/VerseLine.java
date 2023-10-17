package com.tac550.tonewriter.model;

import com.tac550.tonewriter.util.TWUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VerseLine {

	private final String line;

	private final List<String> syllables;

	public VerseLine(String line_text) {
		line = TWUtils.applySmartQuotes(line_text);
		syllables = splitSyllables(line_text);
	}

	private List<String> splitSyllables(String line) {
		List<String> new_syllables = new ArrayList<>(Arrays.asList(
				// Em-dashes (\u2014), minuses (-), and spaces ( ) are acceptable syllable separators.
				// To keep the separators in the resulting strings, place an underscore before each occurrence of these
				// and split at the underscores.
				line.replace("\u2014", "_\u2014").replace("-", "_-")
						.replace(" ", "_ ").split("_")));

		// Filter out syllables that are just single hyphens and apply smart quotes to each syllable.
		return new_syllables.stream().filter(str -> !str.equals("-")).map(TWUtils::applySmartQuotes).toList();
	}

	public String getLine() {
		return TWUtils.reverseSmartQuotes(line).replace("\u2014", "-");
	}
	public List<String> getSyllables() {
		return syllables;
	}

}
