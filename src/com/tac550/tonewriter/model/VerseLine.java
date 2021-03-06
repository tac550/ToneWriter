package com.tac550.tonewriter.model;

import com.tac550.tonewriter.util.TWUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VerseLine {

	private String line;

	private List<String> syllables;

	public VerseLine(String line_text) {
		refreshLine(line_text);
	}

	private void refreshLine(String line) {
		this.line = TWUtils.applySmartQuotes(line);
		syllables = splitSyllables(line);
	}

	private List<String> splitSyllables(String line) {
		List<String> new_syllables = new ArrayList<>(Arrays.asList(
				line.replace("-", "_-").replace(" ", "_ ").split("_")));

		return new_syllables.stream().map(TWUtils::applySmartQuotes).collect(Collectors.toList());
	}

	public String getLine() {
		return TWUtils.reverseSmartQuotes(line);
	}
	public List<String> getSyllables() {
		return syllables;
	}

}
