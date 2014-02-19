package org.ironrhino.core.chart.openflashchart.elements;

import java.util.Arrays;
import java.util.List;

public class ShapeChart extends Element {

	private static final long serialVersionUID = -5491121778724754279L;
	private String colour;

	public ShapeChart() {
		this(null);
	}

	public ShapeChart(String colour) {
		super("shape");
		setColour(colour);
	}

	public void addPoints(Point... points) {
		getValues().addAll(Arrays.asList(points));

	}

	public void addPoints(List<Point> points) {
		getValues().addAll(points);

	}

	public String getColour() {
		return colour;
	}

	public void setColour(String colour) {
		this.colour = colour;

	}

}