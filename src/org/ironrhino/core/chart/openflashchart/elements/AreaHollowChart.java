package org.ironrhino.core.chart.openflashchart.elements;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AreaHollowChart extends LineChart {

	private static final long serialVersionUID = 6023248458467056064L;

	private static transient final Float DEFAULT_ALPHA = 0.35f;

	@JsonProperty("fill-alpha")
	private Float fillAlpha;
	private String fill;

	public AreaHollowChart() {
		super("area_hollow");
		setFillAlpha(DEFAULT_ALPHA);
	}

	public Float getFillAlpha() {
		return fillAlpha;
	}

	public void setFillAlpha(Float fillAlpha) {
		this.fillAlpha = fillAlpha;

	}

	public String getFill() {
		return fill;
	}

	public void setFill(String fill) {
		this.fill = fill;
	}

}