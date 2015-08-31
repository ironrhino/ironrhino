package org.ironrhino.core.chart.openflashchart.axis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class YAxisLabels extends Label {

	private static final long serialVersionUID = -6134375829177947590L;
	private Integer steps;
	private List<Object> labels;

	public YAxisLabels() {
	}

	public YAxisLabels(String... labels) {
		addLabels(labels);
	}

	public YAxisLabels(List<String> labels) {
		addLabels(labels.toArray(new String[0]));
	}

	public List<Object> getLabels() {
		return labels;
	}

	public void addLabels(String... labels) {
		checkLabels();
		this.labels.addAll(Arrays.asList(labels));

	}

	public void addLabels(Label... labels) {
		checkLabels();
		this.labels.addAll(Arrays.asList(labels));

	}

	public void addLabels(List<Label> labels) {
		checkLabels();
		this.labels.addAll(labels);

	}

	public void setSteps(Integer steps) {
		this.steps = steps;

	}

	public Integer getSteps() {
		return steps;
	}

	private synchronized void checkLabels() {
		if (labels == null)
			labels = new ArrayList<>();
	}
}
