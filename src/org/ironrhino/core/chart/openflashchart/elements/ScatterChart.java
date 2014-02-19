package org.ironrhino.core.chart.openflashchart.elements;

import java.util.Arrays;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public class ScatterChart extends Element {

	private static final String TYPE = "scatter";
	private static final long serialVersionUID = 3029567780918048503L;
	private String colour;
	@JsonProperty("dot-size")
	private Integer dotSize;

	public ScatterChart() {
		super(TYPE);
	}

	public ScatterChart(Style style) {
		super(style.getStyle());
	}

	public void addPoints(Point... points) {
		getValues().addAll(Arrays.asList(points));

	}

	public void addPoint(Number x, Number y) {
		addPoints(new Point(x, y));
	}

	public void addPoints(Collection<Point> points) {
		getValues().addAll(points);

	}

	public String getColour() {
		return colour;
	}

	public void setColour(String colour) {
		this.colour = colour;
	}

	public Integer getDotSize() {
		return dotSize;
	}

	public void setDotSize(Integer dotSize) {
		this.dotSize = dotSize;
	}

	public static enum Style {
		NORMAL("scatter"), LINE("scatter_line");

		private String style;

		Style(String style) {
			this.style = style;
		}

		public String getStyle() {
			return style;
		}

		@Override
		@JsonValue
		public String toString() {
			return style;
		}
	}
}
