package org.ironrhino.core.chart.openflashchart.axis;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class Axis implements Serializable {

	private static final long serialVersionUID = 4823643361437691998L;
	private Integer stroke;
	private String colour;
	@JsonProperty("grid-colour")
	private String gridColour;
	private Integer steps;
	private Integer offset;
	private Integer _3d;
	private Number min;
	private Number max;

	public Integer getStroke() {
		return stroke;
	}

	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	public void setStroke(Integer stroke) {
		this.stroke = stroke;
	}

	public String getColour() {
		return colour;
	}

	public void setColour(String colour) {
		this.colour = colour;

	}

	public String getGridColour() {
		return gridColour;
	}

	public void setGridColour(String gridColour) {
		this.gridColour = gridColour;

	}

	public Integer getSteps() {
		return steps;
	}

	public void setSteps(Integer steps) {
		this.steps = steps;

	}

	public Integer getOffset() {
		return offset;
	}

	public void setOffset(Boolean offset) {
		if (offset == null)
			this.offset = 0;
		else
			this.offset = offset ? 1 : 0;

	}

	public Integer get3D() {
		return _3d;
	}

	public void set3D(Integer _3d) {
		this._3d = _3d;

	}

	public Number getMin() {
		return min;
	}

	public void setMin(Number min) {
		this.min = min;

	}

	public Number getMax() {
		return max;
	}

	public void setMax(Number max) {
		this.max = max;

	}

	public void setRange(Number min, Number max, Integer step) {
		setMin(min);
		setMax(max);
		setSteps(step);

	}

	public void setRange(Number min, Number max) {
		setRange(min, max, getSteps());
	}

}
