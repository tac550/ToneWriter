package com.tac550.tonewriter.model;

import java.util.ArrayList;
import java.util.List;

public class AssignmentLine {

	private final ChantPhrase selectedChantLine;
	private final List<AssignmentSyllable> syllables;

	private final String beforeBar;
	private final String afterBar;

	private final boolean separator;
	private final boolean systemBreakingDisabled;

	public AssignmentLine(ChantPhrase selected_chant_line, List<AssignmentSyllable> syllables, String before_bar,
	                      String after_bar, boolean separator, boolean system_breaking_disabled) {
		this.selectedChantLine = selected_chant_line; this.syllables = syllables; this.beforeBar = before_bar;
		this.afterBar = after_bar; this.separator = separator; this.systemBreakingDisabled = system_breaking_disabled;
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
	public boolean isSeparator() {
		return separator;
	}
	public boolean isSystemBreakingDisabled() {
		return systemBreakingDisabled;
	}

	public static class AssignmentLineBuilder {

		private ChantPhrase _selectedChantLine = null;
		private List<AssignmentSyllable> _syllables = new ArrayList<>();

		private String _beforeBar = "";
		private String _afterBar = "";

		private boolean _separator = false;
		private boolean _systemBreakingDisabled = false;

		public AssignmentLineBuilder() { }

		public AssignmentLine buildAssignmentLine() {
			return new AssignmentLine(_selectedChantLine, _syllables, _beforeBar, _afterBar, _separator, _systemBreakingDisabled);
		}

		public AssignmentLineBuilder selectedChantLine(ChantPhrase _selectedChantLine) {
			this._selectedChantLine = _selectedChantLine;
			return this;
		}
		public AssignmentLineBuilder syllables(List<AssignmentSyllable> _syllables) {
			this._syllables = _syllables;
			return this;
		}
		public AssignmentLineBuilder beforeBar(String _beforeBar) {
			this._beforeBar = _beforeBar;
			return this;
		}
		public AssignmentLineBuilder afterBar(String _afterBar) {
			this._afterBar = _afterBar;
			return this;
		}
		public AssignmentLineBuilder separator(boolean _separator) {
			this._separator = _separator;
			return this;
		}
		public AssignmentLineBuilder systemBreakDisabled(boolean _systemBreakingDisabled) {
			this._systemBreakingDisabled = _systemBreakingDisabled;
			return this;
		}

	}
}
