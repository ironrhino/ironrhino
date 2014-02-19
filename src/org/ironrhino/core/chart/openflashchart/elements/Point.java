package org.ironrhino.core.chart.openflashchart.elements;

import java.io.Serializable;

public class Point implements Serializable {

	private static final long serialVersionUID = 3966608162231061139L;
	private final Number x;
	private final Number y;

	public Point(Number x, Number y) {
		this.x = x;
		this.y = y;
	}

	public Number getX() {
		return x;
	}

	public Number getY() {
		return y;
	}
}
