package com.tac550.tonewriter.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectItem {

	public enum TitleType {
		NORMAL, LARGE, HIDDEN;
		@Override public String toString() {
			return switch (this) {
				case NORMAL -> "Normal";
				case LARGE -> "Large";
				case HIDDEN -> "Hidden";
			};
		}
	}

	private final List<AssignmentLine> assignmentLines;
	private final Tone associatedTone;
	private final File originalToneFile;
	private final File toneLoadedFrom;
	private final boolean toneEdited;

	private final String titleText;
	private final TitleType titleType;
	private final String subtitleText;
	private final String verseAreaText;
	private final String topVersePrefix;
	private final String bottomVersePrefix;
	private final String topVerse;
	private final String bottomVerse;

	private final boolean hideToneHeader;
	private final boolean pageBreakBeforeItem;
	private final int extendedTextSelection;
	private final boolean breakExtendedTextOnlyOnBlank;

	public ProjectItem(List<AssignmentLine> assign_lines, Tone assoc_tone, File orig_tone_file, File tone_loaded_from,
	                   boolean tone_edited, String title_text, TitleType title_type, String subtitle_text,
	                   String verse_area_text, String top_prefix, String bottom_prefix, String top_verse, String bottom_verse,
					   boolean hide_tone_header, boolean break_before, int extended_text_sel, boolean break_only_on_blank) {
		this.assignmentLines = assign_lines; this.associatedTone = assoc_tone; this.originalToneFile = orig_tone_file;
		this.toneLoadedFrom = tone_loaded_from; this.toneEdited = tone_edited;this.titleText = title_text; this.titleType = title_type;
		this.subtitleText = subtitle_text; this.verseAreaText = verse_area_text; this.topVersePrefix = top_prefix;
		this.bottomVersePrefix = bottom_prefix; this.topVerse = top_verse; this.bottomVerse = bottom_verse;
		this.hideToneHeader = hide_tone_header; this.pageBreakBeforeItem = break_before;
		this.extendedTextSelection = extended_text_sel; this.breakExtendedTextOnlyOnBlank = break_only_on_blank;
	}

	public List<AssignmentLine> getAssignmentLines() {
		return assignmentLines;
	}
	public Tone getAssociatedTone() {
		return associatedTone;
	}
	public File getOriginalToneFile() {
		return originalToneFile;
	}
	public File getLoadedToneFile() {
		return toneLoadedFrom;
	}
	public boolean isToneEdited() {
		return toneEdited;
	}
	public String getTitleText() {
		return titleText;
	}
	public TitleType getTitleType() {
		return titleType;
	}
	public String getSubtitleText() {
		return subtitleText;
	}
	public String getVerseAreaText() {
		return verseAreaText;
	}
	public String getTopVersePrefix() {
		return topVersePrefix;
	}
	public String getBottomVersePrefix() {
		return bottomVersePrefix;
	}
	public String getTopVerse() {
		return topVerse;
	}
	public String getBottomVerse() {
		return bottomVerse;
	}
	public boolean isHideToneHeader() {
		return hideToneHeader;
	}
	public boolean isPageBreakBeforeItem() {
		return pageBreakBeforeItem;
	}
	public int getExtendedTextSelection() {
		return extendedTextSelection;
	}
	public boolean isBreakExtendedTextOnlyOnBlank() {
		return breakExtendedTextOnlyOnBlank;
	}

	public static class ProjectItemBuilder {

		private List<AssignmentLine> _assignmentLines = new ArrayList<>();
		private Tone _associatedTone = null;
		private File _originalToneFile = null;
		private File _toneLoadedFrom = null;
		private boolean _toneEdited = false;

		private String _titleText = "";
		private TitleType _titleType = TitleType.NORMAL;
		private String _subtitleText = "";
		private String _verseAreaText = "";
		private String _topVersePrefix = "Reader:";
		private String _bottomVersePrefix = "Reader:";
		private String _topVerse = "";
		private String _bottomVerse = "";

		private boolean _hideToneHeader = false;
		private boolean _pageBreakBeforeItem = false;
		private int _extendedTextSelection = 0;
		private boolean _breakExtendedTextOnlyOnBlank = false;

		public ProjectItemBuilder() { }

		public ProjectItem buildProjectItem() {
			return new ProjectItem(_assignmentLines, _associatedTone, _originalToneFile, _toneLoadedFrom, _toneEdited, _titleText, _titleType,
					_subtitleText, _verseAreaText, _topVersePrefix, _bottomVersePrefix, _topVerse, _bottomVerse, _hideToneHeader,
					_pageBreakBeforeItem, _extendedTextSelection, _breakExtendedTextOnlyOnBlank);
		}

		public ProjectItemBuilder assignmentLines(List<AssignmentLine> _assignmentLines) {
			this._assignmentLines = _assignmentLines;
			return this;
		}
		public ProjectItemBuilder associatedTone(Tone _associatedTone) {
			this._associatedTone = _associatedTone;
			return this;
		}
		public ProjectItemBuilder originalToneFile(File _originalToneFile) {
			this._originalToneFile = _originalToneFile;
			return this;
		}
		public ProjectItemBuilder toneLoadedFrom(File _toneLoadedFrom) {
			this._toneLoadedFrom = _toneLoadedFrom;
			return this;
		}
		public ProjectItemBuilder toneEdited(boolean _toneEdited) {
			this._toneEdited = _toneEdited;
			return this;
		}
		public ProjectItemBuilder titleText(String _titleText) {
			this._titleText = _titleText;
			return this;
		}
		public ProjectItemBuilder titleType(TitleType _titleType) {
			this._titleType = _titleType;
			return this;
		}
		public ProjectItemBuilder subtitleText(String _subtitleText) {
			this._subtitleText = _subtitleText;
			return this;
		}
		public ProjectItemBuilder verseAreaText(String _verseAreaText) {
			this._verseAreaText = _verseAreaText;
			return this;
		}
		public ProjectItemBuilder topVersePrefix(String _topVersePrefix) {
			this._topVersePrefix = _topVersePrefix;
			return this;
		}
		public ProjectItemBuilder bottomVersePrefix(String _bottomVersePrefix) {
			this._bottomVersePrefix = _bottomVersePrefix;
			return this;
		}
		public ProjectItemBuilder topVerse(String _topVerse) {
			this._topVerse = _topVerse;
			return this;
		}
		public ProjectItemBuilder bottomVerse(String _bottomVerse) {
			this._bottomVerse = _bottomVerse;
			return this;
		}
		public ProjectItemBuilder hideToneHeader(boolean _hideToneHeader) {
			this._hideToneHeader = _hideToneHeader;
			return this;
		}
		public ProjectItemBuilder breakBeforeItem(boolean _pageBreakBeforeItem) {
			this._pageBreakBeforeItem = _pageBreakBeforeItem;
			return this;
		}
		public ProjectItemBuilder extendedTextSelection(int _extendedTextSelection) {
			this._extendedTextSelection = _extendedTextSelection;
			return this;
		}
		public ProjectItemBuilder breakExtendedTextOnlyOnBlank(boolean _breakExtendedTextOnlyOnBlank) {
			this._breakExtendedTextOnlyOnBlank = _breakExtendedTextOnlyOnBlank;
			return this;
		}
	}
}
