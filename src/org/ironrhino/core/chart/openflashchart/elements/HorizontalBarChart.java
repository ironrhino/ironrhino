package org.ironrhino.core.chart.openflashchart.elements;

import java.util.Arrays;
import java.util.List;

public class HorizontalBarChart extends Element {

	private static final long serialVersionUID = 3320580794787784639L;
	private String colour;

	public HorizontalBarChart() {
		super("hbar");
	}

	public String getColour() {
		return colour;
	}

	public void setColour(String colour) {
		this.colour = colour;

	}

	public void addBars(Bar... bars) {
		getValues().addAll(Arrays.asList(bars));

	}

	public void addBars(List<Bar> bars) {
		getValues().addAll(bars);

	}

	public void addValues(Number... rightValues) {
		Bar[] values = new Bar[rightValues.length];
		for (int i = 0; i < rightValues.length; ++i) {
			values[i] = new Bar(rightValues[i]);
		}
		addBars(values);
	}

	public void addValues(List<Number> rightValues) {
		for (Number number : rightValues) {
			if (number != null) {
				this.addBars(new Bar(number));
			}
		}

	}

	public void addBar(Number left, Number right) {
		addBars(new Bar(left, right));
	}

	public static class Bar {
		private final Number right;
		private Number left;

		public Bar(Number right) {
			this(null, right);
		}

		public Bar(Number left, Number right) {
			if (right == null)
				throw new NullPointerException("Field is mandatory.");
			this.right = right;
			setLeft(left);
		}

		public Number getRight() {
			return right;
		}

		public Number getLeft() {
			return left;
		}

		public void setLeft(Number left) {
			this.left = left;

		}
	}
}
