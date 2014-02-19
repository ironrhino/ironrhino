package org.ironrhino.core.chart.openflashchart.elements;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;

public class BarChart extends Element {

	private static final long serialVersionUID = 6695611795831460343L;
	private String colour;

	public BarChart() {
		this(Style.NORMAL);
	}

	public BarChart(Style style) {
		super(style.getStyle());
	}

	protected BarChart(String style) {
		super(style);
	}

	public void addValues(Number... values) {
		addValues(Arrays.asList(values));
	}

	public void addValues(List<Number> values) {
		for (Number number : values) {
			if (number != null) {
				this.addBars(new Bar(number));
			}
		}

	}

	public void addBars(Bar... bars) {
		addBars(Arrays.asList(bars));

	}

	public void addBars(List<Bar> bars) {
		getValues().addAll(bars);

	}

	public String getColour() {
		return colour;
	}

	public void setColour(String colour) {
		this.colour = colour;

	}

	public static enum Style {
		NORMAL("bar"), THREED("bar_3d"), GLASS("bar_glass");

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

	public static class Bar implements Serializable {

		private static final long serialVersionUID = 787271112021579206L;
		private Number top;
		private Number bottom;
		private String colour;
		private String tip;

		public Bar(Number top, Number bottom, String colour) {
			setTop(top);
			setBottom(bottom);
			setColour(colour);
		}

		public Bar(Number top, Number bottom) {
			this(top, bottom, null);
		}

		public Bar(Number top, String colour) {
			this(top, null, colour);
		}

		public Bar(Number top) {
			this(top, null, null);
		}

		public Number getTop() {
			return top;
		}

		public void setTop(Number top) {
			this.top = top;

		}

		public Number getBottom() {
			return bottom;
		}

		public void setBottom(Number bottom) {
			this.bottom = bottom;

		}

		public String getColour() {
			return colour;
		}

		public void setColour(String colour) {
			this.colour = colour;

		}

		public String getTip() {
			return tip;
		}

		public void setTip(String tip) {
			this.tip = tip;
		}

	}
}
