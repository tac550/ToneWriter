package com.tac550.tonewriter.model;

import java.util.List;

public class AssignmentLine {

	private final ChantPhrase selectedChantLine;
	private final List<AssignmentSyllable> syllables;

	private final String beforeBar;
	private final String afterBar;

	private final boolean systemBreakingDisabled;

	public AssignmentLine(ChantPhrase selected_chant_line, List<AssignmentSyllable> syllables, String before_bar,
	                      String after_bar, boolean system_breaking_disabled) {
		this.selectedChantLine = selected_chant_line; this.syllables = syllables; this.beforeBar = before_bar;
		this.afterBar = after_bar; this.systemBreakingDisabled = system_breaking_disabled;
	}

	public ChantPhrase getSelectedChantLine() {
		return selectedChantLine;
	}
	public List<AssignmentSyllable> getSyllables() {
		return syllables;
	}
	public String getBeforeBar() {
		return beforeBar;
	}
	public String getAfterBar() {
		return afterBar;
	}
	public boolean isSystemBreakingDisabled() {
		return systemBreakingDisabled;
	}
}
