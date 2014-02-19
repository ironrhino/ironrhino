package org.ironrhino.core.chart.openflashchart.axis;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class XAxis extends Axis {

	private static final long serialVersionUID = -7007897621631089309L;
	@JsonProperty("tick-height")
	private Integer tickHeight;
	private XAxisLabels labels = new XAxisLabels();

	public void setTickHeight(Integer tickHeight) {
		this.tickHeight = tickHeight;
	}

	public Integer getTickHeight() {
		return tickHeight;
	}

	public XAxisLabels getLabels() {
		return labels;
	}

	public void setXAxisLabels(XAxisLabels labels) {
		this.labels = labels;
	}

	public void setLabels(String... labels) {
		this.labels = new XAxisLabels(labels);
	}

	public void setLabels(List<String> labels) {
		this.labels = new XAxisLabels(labels);
	}

	public void addLabels(String... labels) {
		this.labels.addLabels(labels);
	}

	public void addLabels(Label... labels) {
		this.labels.addLabels(labels);
	}

	public void addLabels(List<Label> labels) {
		this.labels.addLabels(labels);
	}
}
