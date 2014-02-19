package org.ironrhino.core.chart.openflashchart.axis;

import java.util.List;

public class YAxis extends Axis {

	private static final long serialVersionUID = 7471159737831995334L;
	private Integer tick_length;
	private YAxisLabels labels;

	public Integer getTick_length() {
		return tick_length;
	}

	public void setTick_length(Integer tickLength) {
		tick_length = tickLength;
	}

	public void setLabels(YAxisLabels labels) {
		this.labels = labels;
	}

	public void setLabels(String... labels) {
		this.labels = new YAxisLabels(labels);

	}

	public void setLabels(List<String> labels) {
		this.labels = new YAxisLabels(labels);

	}

	public void addLabels(String... labels) {
		if (this.labels == null) {
			this.labels = new YAxisLabels();
		}
		this.labels.addLabels(labels);

	}

	public void addLabels(List<Label> labels) {
		if (this.labels == null) {
			this.labels = new YAxisLabels();
		}
		this.labels.addLabels(labels);

	}

	public YAxisLabels getLabels() {
		if (this.labels == null) {
			this.labels = new YAxisLabels();
		}
		return this.labels;
	}
}
