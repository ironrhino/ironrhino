package org.ironrhino.core.chart.openflashchart.elements;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public class LineChart extends Element {

	private static final long serialVersionUID = 8807130855547088579L;
	private static transient final Integer DEFAULT_FONTSIZE = 10;
	private Integer width;
	@JsonProperty("dot-size")
	private Integer dotSize;
	@JsonProperty("halo-size")
	private Integer haloSize;
	private String colour;
	private String axis;

	public String getAxis() {
		return axis;
	}

	public void setAxis(String axis) {
		this.axis = axis;
	}

	public String getYaxis() {
		return axis;
	}

	public void setYAxis(String yAxis) {
		this.axis = yAxis;
	}

	public void setRightYAxis() {
		setYAxis("right");
	}

	public LineChart() {
		this(Style.NORMAL);
	}

	public LineChart(Style style) {
		this(style.getStyle());
	}

	protected LineChart(String type) {
		super(type);
		setFontSize(DEFAULT_FONTSIZE);
	}

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;

	}

	public Integer getDotSize() {
		return dotSize;
	}

	public void setDotSize(Integer dotSize) {
		this.dotSize = dotSize;

	}

	public String getColour() {
		return colour;
	}

	public void setColour(String colour) {
		this.colour = colour;

	}

	public void addValues(Number... values) {
		addValues(Arrays.asList(values));
	}

	public void addValues(List<Number> values) {

		for (Number number : values) {
			if (number == null) {
				getValues().add(new NullElement());
			} else {
				getValues().add(number);
			}
		}

	}

	public void addDots(Dot... dots) {
		addDots(Arrays.asList(dots));
	}

	public void addDots(List<Dot> dots) {
		getValues().addAll(dots);

	}

	public Integer getHaloSize() {
		return haloSize;
	}

	public void setHaloSize(Integer haloSize) {
		this.haloSize = haloSize;

	}

	public static enum Style {
		NORMAL("line"), DOT("line_dot"), HOLLOW("line_hollow");

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
