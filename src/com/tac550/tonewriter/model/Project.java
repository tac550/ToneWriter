package com.tac550.tonewriter.model;

import com.tac550.tonewriter.view.TopSceneController;

import java.util.ArrayList;
import java.util.List;

public class Project {

	private List<ProjectItem> items;

	private String title;
	private String paperSize;
	private boolean noHeader;
	private boolean evenSpread;

	private String[] marginInfo;

	public Project(List<ProjectItem> items, String title, String paper_size, boolean no_header, boolean even_spread,
	               String[] margin_info) {
		this.items = items; this.title = title; this.paperSize = paper_size; this.noHeader = no_header;
		this.evenSpread = even_spread; this.marginInfo = margin_info;
	}

	public List<ProjectItem> getItems() {
		return items;
	}
	public String getTitle() {
		return title;
	}
	public String getPaperSize() {
		return paperSize;
	}
	public boolean isNoHeader() {
		return noHeader;
	}
	public boolean isEvenSpread() {
		return evenSpread;
	}
	public String[] getMarginInfo() {
		return marginInfo;
	}

	public static class ProjectBuilder {
		private List<ProjectItem> _items = new ArrayList<>();

		private String _title = "Unnamed Project";
		private String _paperSize = TopSceneController.getDefaultPaperSize();
		private boolean _noHeader = false;
		private boolean _evenSpread = true;

		private static final String marginStr = String.valueOf(TopSceneController.DEFAULT_MARGIN_SIZE);
		private static final String unitStr = TopSceneController.DEFAULT_MARGIN_UNITS;
		private String[] _marginInfo = {marginStr, unitStr, marginStr, unitStr, marginStr, unitStr, marginStr, unitStr};

		public ProjectBuilder() { }

		public Project buildProject() {
			return new Project(_items, _title, _paperSize, _noHeader, _evenSpread, _marginInfo);
		}

		public ProjectBuilder items(List<ProjectItem> _items) {
			this._items = _items;
			return this;
		}
		public ProjectBuilder title(String _title) {
			this._title = _title;
			return this;
		}
		public ProjectBuilder paperSize(String _paperSize) {
			this._paperSize = _paperSize;
			return this;
		}
		public ProjectBuilder noHeader(boolean _noHeader) {
			this._noHeader = _noHeader;
			return this;
		}
		public ProjectBuilder evenSpread(boolean _evenSpread) {
			this._evenSpread = _evenSpread;
			return this;
		}
		public ProjectBuilder marginInfo(String[] _marginInfo) {
			this._marginInfo = _marginInfo;
			return this;
		}
	}
}
