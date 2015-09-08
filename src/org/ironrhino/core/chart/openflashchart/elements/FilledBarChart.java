package org.ironrhino.core.chart.openflashchart.elements;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FilledBarChart extends BarChart {

	private static final long serialVersionUID = 3471991868191065273L;
	private static final transient String TYPE = "bar_filled";
	@JsonProperty("outline-colour")
	private String outlineColour;

	public FilledBarChart() {
		super(TYPE);
	}

	public FilledBarChart(String colour, String outlineColour) {
		super(TYPE);
		setColour(colour);
		setOutlineColour(outlineColour);
	}

	protected FilledBarChart(String style) {
		super(style);
	}

	public String getOutlineColour() {
		return outlineColour;
	}

	public void setOutlineColour(String outlineColour) {
		this.outlineColour = outlineColour;

	}

	public static class Bar extends BarChart.Bar {

		private static final long serialVersionUID = 8679477620059697844L;
		@JsonProperty("outline-colour")
		private String outlineColour;

		public Bar(Number top, Number bottom) {
			super(top, bottom);
		}

		public Bar(Number top, Number bottom, String colour, String outlineColour) {
			super(top, bottom);
			setColour(colour);
			setOutlineColour(outlineColour);
		}

		public Bar(Number top) {
			super(top);
		}

		public void setOutlineColour(String outlineColour) {
			this.outlineColour = outlineColour;

		}

		public String getOutlineColour() {
			return outlineColour;
		}
	}
}
