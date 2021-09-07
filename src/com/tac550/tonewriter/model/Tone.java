package com.tac550.tonewriter.model;

import java.util.ArrayList;
import java.util.List;

public class Tone {

	private final String keySignature;
	private final String toneText;
	private final String composerText;
	private final boolean manuallyAssignPhrases;

	private final List<ChantPhrase> chantPhrases;

	private final String firstRepeated;

	public Tone(String key_signature, String tone_text, String composer_text, boolean manual_assignment,
	            List<ChantPhrase> chant_phrases, String first_repeated) {
		this.keySignature = key_signature; this.toneText = tone_text; this.composerText = composer_text;
		this.manuallyAssignPhrases = manual_assignment; this.chantPhrases = chant_phrases;
		this.firstRepeated = first_repeated;
	}

	public String getKeySignature() {
		return keySignature;
	}
	public String getToneText() {
		return toneText;
	}
	public String getComposerText() {
		return composerText;
	}
	public boolean isManuallyAssignPhrases() {
		return manuallyAssignPhrases;
	}
	public List<ChantPhrase> getChantPhrases() {
		return chantPhrases;
	}
	public String getFirstRepeated() {
		return firstRepeated;
	}

	public static class ToneBuilder {
		private String _keySignature = "C major";
		private String _toneText = "";
		private String _composerText = "";
		private boolean _manuallyAssignPhrases = false;

		private List<ChantPhrase> _chantPhrases = new ArrayList<>();

		private String _firstRepeated = "";

		public ToneBuilder() { }

		public Tone buildTone() {
			return new Tone(_keySignature, _toneText, _composerText, _manuallyAssignPhrases, _chantPhrases, _firstRepeated);
		}

		public ToneBuilder keySignature(String _keySignature) {
			this._keySignature = _keySignature;
			return this;
		}
		public ToneBuilder toneText(String _toneText) {
			this._toneText = _toneText;
			return this;
		}
		public ToneBuilder composerText(String _composerText) {
			this._composerText = _composerText;
			return this;
		}
		public ToneBuilder manualAssignment(boolean _manualAssignment) {
			this._manuallyAssignPhrases = _manualAssignment;
			return this;
		}
		public ToneBuilder chantPhrases(List<ChantPhrase> _chantPhrases) {
			this._chantPhrases = _chantPhrases;
			return this;
		}
		public ToneBuilder firstRepeated(String _firstRepeated) {
			this._firstRepeated = _firstRepeated;
			return this;
		}
	}
}
