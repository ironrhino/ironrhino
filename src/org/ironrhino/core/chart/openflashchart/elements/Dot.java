package org.ironrhino.core.chart.openflashchart.elements;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Dot implements Serializable {

	private static final long serialVersionUID = 8385409725410934649L;
	@JsonProperty("halo-size")
	private Integer haloSize;
	@JsonProperty("dot-size")
	private Integer dotSize;
	private Number value;
	private String colour;

	public Dot(Number value) {
		this(value, null, null, null);
	}

	public Dot(Number value, String colour) {
		this(value, colour, null, null);
	}

	public Dot(Number value, String colour, Integer dotSize, Integer haloSize) {
		setValue(value);
		setColour(colour);
		setDotSize(dotSize);
		setHaloSize(haloSize);
	}

	public Integer getHaloSize() {
		return haloSize;
	}

	public void setHaloSize(Integer haloSize) {
		this.haloSize = haloSize;

	}

	public Integer getDotSize() {
		return dotSize;
	}

	public void setDotSize(Integer dotSize) {
		this.dotSize = dotSize;

	}

	public Number getValue() {
		return value;
	}

	public void setValue(Number value) {
		this.value = value;

	}

	public String getColour() {
		return colour;
	}

	public void setColour(String colour) {
		this.colour = colour;

	}
}
