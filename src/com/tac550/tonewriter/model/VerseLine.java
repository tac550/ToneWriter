package com.tac550.tonewriter.model;

import com.tac550.tonewriter.util.TWUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VerseLine {

	private String line;

	private List<String> syllables;

	public VerseLine(String line_text) {
		line = TWUtils.applySmartQuotes(line_text);
		syllables = splitSyllables(line);
	}

	private List<String> splitSyllables(String line) {
		List<String> new_syllables = new ArrayList<>(Arrays.asList(
				line.replace("-", "_-").replace(" ", "_ ").split("_")));

		return new_syllables.stream().map(TWUtils::applySmartQuotes).toList();
	}

	public String getLine() {
		return TWUtils.reverseSmartQuotes(line);
	}
	public List<String> getSyllables() {
		return syllables;
	}

}
